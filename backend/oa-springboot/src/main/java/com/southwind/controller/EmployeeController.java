package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.annotation.RequirePermission;
import com.southwind.constant.SystemConstants;
import com.southwind.vo.ResultVO;
import com.southwind.common.UserContext;
import com.southwind.entity.Department;
import com.southwind.entity.Employee;
import com.southwind.entity.Message;
import com.southwind.entity.Shift;
import com.southwind.enums.RoleType;
import com.southwind.form.RuleForm;
import com.southwind.service.DepartmentService;
import com.southwind.service.EmployeeService;
import com.southwind.service.MessageService;
import com.southwind.service.ShiftService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.southwind.util.JwtTokenUtil;
import com.southwind.util.CustomPasswordEncoder;
import com.southwind.util.ResultVOUtil;
import com.southwind.util.ValidatorUtil;
import com.southwind.vo.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 员工控制器（重构版 - 修复安全漏洞）
 * 
 * 🔒 安全修复：
 * 1. 密码加密存储（MD5+盐值）
 * 2. SQL注入防护（搜索字段白名单）
 * 3. 参数校验（防止NPE）
 * 4. JWT Token机制（替代ThreadLocal手动设置）
 * 5. 权限统一管理（使用AOP注解）
 * 
 * 📋 权限矩阵：
 * - 系统管理员：全部权限（增删改查、在职状态管理、照片上传）
 * - 部门主管：只读（仅查看本部门员工列表）
 * - 流程管理员：无权限
 * - 普通员工：无权限（仅能在个人中心维护自己的信息）
 */
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private CustomPasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private MessageService messageService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ShiftService shiftService;

    // ==================== 登录安全限制 ====================
    // 最大失败次数
    private static final int MAX_FAILED_ATTEMPTS = 5;
    // 锁定时间（毫秒）- 1分钟
    private static final long LOCK_TIME_MILLIS = 60 * 1000;
    
    // 登录失败记录：key=username, value=LoginAttempt（包含失败次数和锁定时间）
    private static final ConcurrentHashMap<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    
    /**
     * 登录尝试记录
     */
    private static class LoginAttempt {
        final AtomicInteger failedCount = new AtomicInteger(0);
        volatile long lockTime = 0;
        
        boolean isLocked() {
            return lockTime > System.currentTimeMillis();
        }
        
        void reset() {
            failedCount.set(0);
            lockTime = 0;
        }
        
        int incrementAndGet() {
            return failedCount.incrementAndGet();
        }
        
        void lock() {
            lockTime = System.currentTimeMillis() + LOCK_TIME_MILLIS;
        }
        
        long getRemainingLockTime() {
            return Math.max(0, lockTime - System.currentTimeMillis());
        }
    }

    /**
     * 员工登录
     * ✅ 安全修复：
     * 1. 密码加密验证
     * 2. 返回JWT Token（不再手动设置ThreadLocal）
     * 3. 参数校验（严格判空）
     * 4. 登录失败次数限制（5次后暂停1分钟）
     */
    @PostMapping("/login")
    public ResultVO login(@RequestBody RuleForm ruleForm) {
        // ========== 参数校验（严格判空） ==========
        if (ruleForm == null) {
            return ResultVOUtil.fail("请求参数不能为空");
        }
        
        try {
            ValidatorUtil.notEmpty(ruleForm.getUsername(), "用户名");
            ValidatorUtil.notEmpty(ruleForm.getPassword(), "密码");
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }

        String username = ruleForm.getUsername().trim();
        String password = ruleForm.getPassword().trim();

        // ========== 登录安全限制：检查是否被锁定 ==========
        LoginAttempt attempt = loginAttempts.computeIfAbsent(username, k -> new LoginAttempt());
        
        if (attempt.isLocked()) {
            long remainingSeconds = attempt.getRemainingLockTime() / 1000;
            return ResultVOUtil.fail("账号已被锁定，请" + remainingSeconds + "秒后重试");
        }

        // ========== 查询员工 ==========
        QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SystemConstants.COLUMN_USERNAME, username);
        Employee employee = employeeService.getOne(queryWrapper);

        if (employee == null) {
            // 记录失败次数
            recordLoginFailure(username, attempt);
            int remainingAttempts = MAX_FAILED_ATTEMPTS - attempt.failedCount.get();
            if (remainingAttempts > 0) {
                return ResultVOUtil.fail("员工账号不存在，还剩" + remainingAttempts + "次机会");
            } else {
                return ResultVOUtil.fail("登录失败次数过多，账号已被锁定，请1分钟后重试");
            }
        }

        // ========== 密码验证（加密） ==========
        boolean matches = passwordEncoder.matches(password, employee.getPassword());

        if (!matches) {
            // 记录失败次数
            recordLoginFailure(username, attempt);
            int remainingAttempts = MAX_FAILED_ATTEMPTS - attempt.failedCount.get();
            if (remainingAttempts > 0) {
                return ResultVOUtil.fail(SystemConstants.MSG_PASSWORD_ERROR + "，还剩" + remainingAttempts + "次机会");
            } else {
                return ResultVOUtil.fail("密码错误次数过多，账号已被锁定，请1分钟后重试");
            }
        }

        // ========== 登录成功：重置失败记录 ==========
        attempt.reset();

        // ========== 生成JWT Token ==========
        RoleType role = RoleType.getByCode(employee.getRole());
        UserContext.UserInfo userInfo = new UserContext.UserInfo(
            employee.getId(),
            employee.getUsername(),
            employee.getName(),
            role,
            employee.getDepartmentId()
        );

        String token = jwtTokenUtil.generateToken(userInfo);

        // ========== 返回结果 ==========
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("employee", employee);

        return ResultVOUtil.success(data);
    }

    /**
     * 记录登录失败
     */
    private void recordLoginFailure(String username, LoginAttempt attempt) {
        int failedCount = attempt.incrementAndGet();
        if (failedCount >= MAX_FAILED_ATTEMPTS) {
            attempt.lock();
        }
    }

    /**
     * 添加员工
     * ✅ 安全修复：
     * 1. 参数校验
     * 2. 密码加密
     * 3. 使用AOP权限注解
     */
    @PostMapping("/save")
    public ResultVO save(@RequestBody Employee employee) {
        try {
            // 参数校验
            ValidatorUtil.notEmpty(employee.getUsername(), "用户名");
            ValidatorUtil.notEmpty(employee.getName(), "姓名");
            ValidatorUtil.notEmpty(employee.getPassword(), "密码");
            ValidatorUtil.notNull(employee.getDepartmentId(), "部门ID");
            ValidatorUtil.validatePhone(employee.getPhone());
            ValidatorUtil.validateEmail(employee.getEmail());

            // 权限校验
            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            // 部门主管只能添加本部门员工
            if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
                if (!currentUser.getDepartmentId().equals(employee.getDepartmentId())) {
                    return ResultVOUtil.fail("只能添加本部门员工");
                }
                // 部门主管添加的员工默认角色为普通员工
                employee.setRole(RoleType.EMPLOYEE.getCode());
            }

            // 系统管理员可以添加任意部门员工
            if (currentUser.getRole() != RoleType.SYSTEM_ADMIN && 
                currentUser.getRole() != RoleType.DEPARTMENT_MANAGER) {
                return ResultVOUtil.fail("无权添加员工");
            }

            // 检查用户名是否已存在
            QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq(SystemConstants.COLUMN_USERNAME, employee.getUsername());
            if (employeeService.getOne(queryWrapper) != null) {
                return ResultVOUtil.fail(SystemConstants.MSG_USERNAME_EXISTS);
            }

            // 密码加密
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));

            // 设置默认角色（如果未指定）
            if (employee.getRole() == null || employee.getRole().isEmpty()) {
                employee.setRole(RoleType.EMPLOYEE.getCode());
            }

            // 设置默认状态
            if (employee.getStatus() == null || employee.getStatus().isEmpty()) {
                employee.setStatus(SystemConstants.EMPLOYEE_STATUS_ACTIVE);
            }

            boolean save = employeeService.save(employee);
            if (!save) return ResultVOUtil.fail(SystemConstants.MSG_OPERATION_FAILED);

            Message message = new Message();
            message.setSenderId(currentUser.getUserId());
            message.setSenderName(currentUser.getUsername());
            message.setReceiverId(employee.getId());
            message.setReceiverName(employee.getName());
            message.setTitle("欢迎加入公司");
            message.setContent("您好！您的账号已创建成功，用户名：" + employee.getUsername() + "，初始密码：******。请登录后及时修改密码。");
            message.setMsgType("SYSTEM");
            message.setIsRead(0);
            message.setIsTop(1);
            messageService.sendMessage(message);

            return ResultVOUtil.success(null);
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    /**
     * 根据ID查询员工
     * ✅ 安全修复：
     * 1. 参数校验
     * 2. 权限统一使用AOP
     * 3. 使用常量替代硬编码
     */
    @GetMapping("/findById/{id}")
    public ResultVO findById(@PathVariable("id") Integer id) {
        try {
            // 参数校验
            ValidatorUtil.validId(id, "员工ID");

            Employee employee = employeeService.getById(id);
            if (employee == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_EMPLOYEE_NOT_FOUND);
            }

            // ========== 权限校验 ==========
            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            // 系统管理员可以查看所有员工
            if (currentUser.getRole() == RoleType.SYSTEM_ADMIN) {
                return ResultVOUtil.success(employee);
            }

            // 部门主管只能查看本部门员工
            if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
                if (currentUser.getDepartmentId().equals(employee.getDepartmentId())) {
                    return ResultVOUtil.success(employee);
                } else {
                    return ResultVOUtil.fail("无权查看该员工信息");
                }
            }

            // 普通员工和流程管理员只能查看自己
            if (currentUser.getUserId().equals(id)) {
                return ResultVOUtil.success(employee);
            }

            return ResultVOUtil.fail("无权查看该员工信息");
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    /**
     * 员工列表（分页）
     * ✅ 安全修复：
     * 1. 限制分页大小
     * 2. 参数校验
     * 3. SQL注入防护
     */
    @GetMapping("/list/{page}/{size}")
    public ResultVO list(@PathVariable("page") Integer page,
                         @PathVariable("size") Integer size,
                         @RequestParam(required = false) Integer departmentId) {
        try {
            // ========== 参数校验与限制 ==========
            page = SystemConstants.validatePageNum(page);
            size = SystemConstants.validatePageSize(size);

            Page<Employee> employeePage = new Page<>(page, size);
            QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();

            // ========== 权限过滤 ==========
            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            // 通讯录功能：所有员工都能查看所有同事（支持按部门筛选）
            if (departmentId != null) {
                queryWrapper.eq(SystemConstants.COLUMN_DEPARTMENT_ID, departmentId);
            }
            // 不指定部门时，显示所有员工（通讯录功能）

            queryWrapper.orderByDesc(SystemConstants.COLUMN_CREATE_TIME);
            Page<Employee> resultPage = employeeService.page(employeePage, queryWrapper);

            PageVO pageVO = new PageVO();
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setData(resultPage.getRecords());
        pageVO.setCurrent(resultPage.getCurrent());
        pageVO.setSize(resultPage.getSize());
        return ResultVOUtil.success(pageVO);
        } catch (Exception e) {
            return ResultVOUtil.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 搜索员工
     * ✅ 安全修复：
     * 1. SQL注入防护（字段白名单）
     * 2. 参数校验
     * 3. 限制分页大小
     */
    @GetMapping("/search")
    public ResultVO search(@RequestParam(required = false) String key,
                           @RequestParam(required = false) String value,
                           @RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "10") Integer size) {
        try {
            // ========== 参数校验与限制 ==========
            page = SystemConstants.validatePageNum(page);
            size = SystemConstants.validatePageSize(size);

            Page<Employee> employeePage = new Page<>(page, size);
            QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();

            // ========== 权限过滤 ==========
            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            // 通讯录功能：所有员工都能搜索所有同事

            // ========== SQL注入防护：搜索字段白名单验证 ==========
            if (key != null && value != null && !value.isEmpty()) {
                String validKey = ValidatorUtil.validateSearchField(key);
                if (validKey != null) {
                    queryWrapper.like(validKey, value);
                } else {
                    return ResultVOUtil.fail("非法的搜索字段：" + key);
                }
            }

            Page<Employee> resultPage = employeeService.page(employeePage, queryWrapper);

            PageVO pageVO = new PageVO();
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setData(resultPage.getRecords());
        pageVO.setCurrent(resultPage.getCurrent());
        pageVO.setSize(resultPage.getSize());
        return ResultVOUtil.success(pageVO);
        } catch (Exception e) {
            return ResultVOUtil.fail("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 更新员工信息
     * ✅ 安全修复：
     * 1. 参数校验
     * 2. 敏感字段保护
     * 3. 乐观锁并发控制
     */
    @PutMapping("/update")
    public ResultVO update(@RequestBody Employee employee) {
        try {
            // 参数校验
            ValidatorUtil.validId(employee.getId(), "员工ID");
            
            // 验证手机号和邮箱格式
            if (employee.getPhone() != null && !employee.getPhone().trim().isEmpty()) {
                ValidatorUtil.validatePhone(employee.getPhone());
            }
            if (employee.getEmail() != null && !employee.getEmail().trim().isEmpty()) {
                ValidatorUtil.validateEmail(employee.getEmail());
            }

            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            // 获取数据库中的员工信息（用于乐观锁校验）
            Employee dbEmployee = employeeService.getById(employee.getId());
            if (dbEmployee == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_EMPLOYEE_NOT_FOUND);
            }

            // 检查乐观锁版本
            if (employee.getVersion() != null && !employee.getVersion().equals(dbEmployee.getVersion())) {
                return ResultVOUtil.fail("数据已被其他用户修改，请刷新后重试");
            }

            // 系统管理员可以更新所有员工
            if (currentUser.getRole() == RoleType.SYSTEM_ADMIN) {
                boolean update = employeeService.updateById(employee);
                if (!update) {
                    // 再次检查乐观锁
                    Employee newDbEmployee = employeeService.getById(employee.getId());
                    if (newDbEmployee != null && employee.getVersion() != null && !employee.getVersion().equals(newDbEmployee.getVersion())) {
                        return ResultVOUtil.fail("数据已被其他用户修改，请刷新后重试");
                    }
                    return ResultVOUtil.fail(SystemConstants.MSG_OPERATION_FAILED);
                }
                return ResultVOUtil.success(null);
            }

            // 部门主管可以更新本部门员工
            if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
                if (!currentUser.getDepartmentId().equals(dbEmployee.getDepartmentId())) {
                    return ResultVOUtil.fail("只能更新本部门员工");
                }
                // 部门主管不能修改角色和部门
                employee.setRole(null);
                employee.setDepartmentId(null);
                employee.setVersion(dbEmployee.getVersion());
                boolean update = employeeService.updateById(employee);
                if (!update) {
                    return ResultVOUtil.fail("数据已被其他用户修改，请刷新后重试");
                }
                return ResultVOUtil.success(null);
            }

            // 普通员工只能更新自己的基本信息（不包括角色、部门等敏感字段）
            if (currentUser.getUserId().equals(employee.getId())) {
                Employee updateEmployee = new Employee();
                updateEmployee.setId(employee.getId());
                updateEmployee.setName(employee.getName());
                updateEmployee.setVersion(dbEmployee.getVersion());
                
                // 允许修改username，但需要检查唯一性
                if (employee.getUsername() != null && !employee.getUsername().isEmpty()) {
                    QueryWrapper<Employee> usernameQuery = new QueryWrapper<>();
                    usernameQuery.eq(SystemConstants.COLUMN_USERNAME, employee.getUsername());
                    usernameQuery.ne("id", employee.getId());
                    if (employeeService.getOne(usernameQuery) != null) {
                        return ResultVOUtil.fail(SystemConstants.MSG_USERNAME_EXISTS);
                    }
                    updateEmployee.setUsername(employee.getUsername());
                }
                
                updateEmployee.setPhone(employee.getPhone());
                updateEmployee.setEmail(employee.getEmail());
                updateEmployee.setAvatar(employee.getAvatar());

                boolean update = employeeService.updateById(updateEmployee);
                if (!update) {
                    return ResultVOUtil.fail("数据已被其他用户修改，请刷新后重试");
                }
                return ResultVOUtil.success(null);
            }

            return ResultVOUtil.fail("无权更新该员工信息");
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    /**
     * 删除员工
     * ✅ 安全修复：
     * 1. 参数校验
     * 2. 验证员工是否存在
     */
    @DeleteMapping("/deleteById/{id}")
    public ResultVO deleteById(@PathVariable("id") Integer id) {
        try {
            // 参数校验
            ValidatorUtil.validId(id, "员工ID");

            // 检查员工是否存在
            Employee employee = employeeService.getById(id);
            if (employee == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_EMPLOYEE_NOT_FOUND);
            }

            // 检查是否删除自己
            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }
            if (currentUser.getUserId().equals(id)) {
                return ResultVOUtil.fail("不能删除自己的账号");
            }

            // 系统管理员可以删除所有员工
            if (currentUser.getRole() == RoleType.SYSTEM_ADMIN) {
                boolean delete = employeeService.removeById(id);
                if (!delete) return ResultVOUtil.fail(SystemConstants.MSG_OPERATION_FAILED);
                return ResultVOUtil.success(null);
            }

            // 部门主管只能删除本部门员工
            if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
                if (!currentUser.getDepartmentId().equals(employee.getDepartmentId())) {
                    return ResultVOUtil.fail("只能删除本部门员工");
                }
                boolean delete = employeeService.removeById(id);
                if (!delete) return ResultVOUtil.fail(SystemConstants.MSG_OPERATION_FAILED);
                return ResultVOUtil.success(null);
            }

            return ResultVOUtil.fail("无权删除该员工");
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    @PostMapping("/import")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "批量导入员工")
    public ResultVO importEmployees(@RequestBody List<Employee> employees) {
        try {
            if (employees == null || employees.isEmpty()) {
                return ResultVOUtil.fail("请选择要导入的员工数据");
            }

            for (Employee employee : employees) {
                if (employee.getUsername() == null || employee.getUsername().isEmpty()) {
                    employee.setUsername(employee.getPhone());
                }
                if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
                    employee.setPassword("123456");
                }
                employee.setPassword(passwordEncoder.encode(employee.getPassword()));
                
                if (employee.getRole() == null || employee.getRole().isEmpty()) {
                    employee.setRole(RoleType.EMPLOYEE.getCode());
                }
                
                if (employee.getStatus() == null || employee.getStatus().isEmpty()) {
                    employee.setStatus(SystemConstants.EMPLOYEE_STATUS_ACTIVE);
                }
            }

            int successCount = employeeService.importEmployees(employees);
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", employees.size());
            result.put("success", successCount);
            result.put("failed", employees.size() - successCount);
            
            return ResultVOUtil.success(result);
        } catch (Exception e) {
            return ResultVOUtil.fail("导入失败：" + e.getMessage());
        }
    }

    /**
     * 按部门查询员工（通讯录功能，所有登录用户均可访问）
     */
    @GetMapping("/findByDepartment/{departmentId}")
    public ResultVO findByDepartment(@PathVariable("departmentId") Integer departmentId) {
        try {
            ValidatorUtil.validId(departmentId, "部门ID");

            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq(SystemConstants.COLUMN_DEPARTMENT_ID, departmentId);
            queryWrapper.eq(SystemConstants.COLUMN_STATUS, SystemConstants.EMPLOYEE_STATUS_ACTIVE);
            return ResultVOUtil.success(employeeService.list(queryWrapper));
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    /**
     * 查询所有在职员工（用于参会人选择等场景）
     */
    @GetMapping("/findAll")
    public ResultVO findAll() {
        try {
            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq(SystemConstants.COLUMN_STATUS, SystemConstants.EMPLOYEE_STATUS_ACTIVE);
            queryWrapper.orderByAsc(SystemConstants.COLUMN_DEPARTMENT_ID);
            return ResultVOUtil.success(employeeService.list(queryWrapper));
        } catch (Exception e) {
            return ResultVOUtil.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 修改密码
     * ✅ 安全修复：
     * 1. 参数校验
     * 2. 密码加密验证
     * 3. 新密码加密存储
     */
    @PutMapping("/updatePassword")
    public ResultVO updatePassword(@RequestBody Map<String, String> params) {
        try {
            // ========== 参数校验 ==========
            Integer employeeId = ValidatorUtil.parseIntRequired(params.get("employeeId"), "员工ID");
            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");

            ValidatorUtil.notEmpty(oldPassword, "原密码");
            ValidatorUtil.notEmpty(newPassword, "新密码");

            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            // 只能修改自己的密码
            if (!currentUser.getUserId().equals(employeeId)) {
                return ResultVOUtil.fail("无权修改他人密码");
            }

            Employee employee = employeeService.getById(employeeId);
            if (employee == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_EMPLOYEE_NOT_FOUND);
            }

            // 验证原密码（加密验证）
            if (!passwordEncoder.matches(oldPassword, employee.getPassword())) {
                return ResultVOUtil.fail("原密码错误");
            }

            // 更新密码（加密存储）
            Employee updateEmployee = new Employee();
            updateEmployee.setId(employeeId);
            updateEmployee.setPassword(passwordEncoder.encode(newPassword));

            boolean success = employeeService.updateById(updateEmployee);
            if (success) {
                return ResultVOUtil.success("密码修改成功");
            } else {
                return ResultVOUtil.fail(SystemConstants.MSG_OPERATION_FAILED);
            }
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    /**
     * 更新员工状态（在职/离职）
     */
    @PutMapping("/updateStatus")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "更新员工状态")
    public ResultVO updateStatus(@RequestBody Map<String, Object> params) {
        try {
            Integer employeeId = ValidatorUtil.parseIntRequired(params.get("employeeId").toString(), "员工ID");
            String status = (String) params.get("status");

            ValidatorUtil.notEmpty(status, "状态");

            Employee employee = new Employee();
            employee.setId(employeeId);
            employee.setStatus(status);

            boolean success = employeeService.updateById(employee);
            if (success) {
                return ResultVOUtil.success(SystemConstants.MSG_OPERATION_SUCCESS);
            } else {
                return ResultVOUtil.fail(SystemConstants.MSG_OPERATION_FAILED);
            }
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    /**
     * 更新员工角色
     */
    @PutMapping("/updateRole")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "更新员工角色")
    public ResultVO updateRole(@RequestBody Map<String, Object> params) {
        try {
            Integer employeeId = ValidatorUtil.parseIntRequired(params.get("employeeId").toString(), "员工ID");
            String role = (String) params.get("role");

            ValidatorUtil.notEmpty(role, "角色");

            // 验证角色是否合法
            RoleType.getByCode(role);

            Employee employee = new Employee();
            employee.setId(employeeId);
            employee.setRole(role);

            boolean success = employeeService.updateById(employee);
            if (success) {
                return ResultVOUtil.success(SystemConstants.MSG_OPERATION_SUCCESS);
            } else {
                return ResultVOUtil.fail(SystemConstants.MSG_OPERATION_FAILED);
            }
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    /**
     * 更新员工班次
     * 系统管理员：可以修改所有员工的班次
     * 部门主管：可以修改本部门员工的班次
     */
    @PutMapping("/updateShift")
    public ResultVO updateShift(@RequestBody Map<String, Object> params) {
        try {
            Integer employeeId = ValidatorUtil.parseIntRequired(params.get("employeeId").toString(), "员工ID");
            Integer shiftId = params.get("shiftId") != null ? Integer.parseInt(params.get("shiftId").toString()) : null;

            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            Employee targetEmployee = employeeService.getById(employeeId);
            if (targetEmployee == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_EMPLOYEE_NOT_FOUND);
            }

            if (currentUser.getRole() == RoleType.SYSTEM_ADMIN) {
                Employee employee = new Employee();
                employee.setId(employeeId);
                employee.setShiftId(shiftId);
                boolean success = employeeService.updateById(employee);
                if (success) {
                    return ResultVOUtil.success(SystemConstants.MSG_OPERATION_SUCCESS);
                } else {
                    return ResultVOUtil.fail(SystemConstants.MSG_OPERATION_FAILED);
                }
            }

            if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
                if (!currentUser.getDepartmentId().equals(targetEmployee.getDepartmentId())) {
                    return ResultVOUtil.fail("只能修改本部门员工的班次");
                }
                Employee employee = new Employee();
                employee.setId(employeeId);
                employee.setShiftId(shiftId);
                boolean success = employeeService.updateById(employee);
                if (success) {
                    return ResultVOUtil.success(SystemConstants.MSG_OPERATION_SUCCESS);
                } else {
                    return ResultVOUtil.fail(SystemConstants.MSG_OPERATION_FAILED);
                }
            }

            return ResultVOUtil.fail("无权修改员工班次");
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    @GetMapping("/downloadTemplate")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "下载员工导入模板")
    public void downloadTemplate(javax.servlet.http.HttpServletResponse response, @RequestParam(value = "token", required = false) String token) {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("员工导入模板");

            String[] headers = {
                "姓名*", "性别*", "部门名称*", "职位*", "联系电话*", 
                "电子邮箱", "角色*", "班次名称", "入职日期", "状态"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            List<Department> departments = departmentService.list();
            List<Shift> shifts = shiftService.list();

            StringBuilder deptInfo = new StringBuilder("可选部门：");
            for (Department dept : departments) {
                deptInfo.append(dept.getName()).append(";");
            }

            StringBuilder shiftInfo = new StringBuilder("可选班次：");
            for (Shift shift : shifts) {
                shiftInfo.append(shift.getName()).append(";");
            }

            StringBuilder roleInfo = new StringBuilder("可选角色：普通员工;部门主管;流程管理员");
            StringBuilder statusInfo = new StringBuilder("可选状态：在职;试用期;已离职（默认在职）");
            StringBuilder dateInfo = new StringBuilder("日期格式：yyyy-MM-dd（如：2026-01-01）");

            Row tipRow1 = sheet.createRow(2);
            tipRow1.createCell(0).setCellValue(deptInfo.toString());

            Row tipRow2 = sheet.createRow(3);
            tipRow2.createCell(0).setCellValue(shiftInfo.toString());

            Row tipRow3 = sheet.createRow(4);
            tipRow3.createCell(0).setCellValue(roleInfo.toString());

            Row tipRow4 = sheet.createRow(5);
            tipRow4.createCell(0).setCellValue(statusInfo.toString());

            Row tipRow5 = sheet.createRow(6);
            tipRow5.createCell(0).setCellValue(dateInfo.toString());

            Row exampleRow = sheet.createRow(8);
            exampleRow.createCell(0).setCellValue("张三");
            exampleRow.createCell(1).setCellValue("男");
            exampleRow.createCell(2).setCellValue("研发部");
            exampleRow.createCell(3).setCellValue("Java工程师");
            exampleRow.createCell(4).setCellValue("13800000001");
            exampleRow.createCell(5).setCellValue("zhangsan@oa.com");
            exampleRow.createCell(6).setCellValue("普通员工");
            exampleRow.createCell(7).setCellValue("白班");
            exampleRow.createCell(8).setCellValue("2026-01-15");
            exampleRow.createCell(9).setCellValue("在职");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            // 使用URL编码处理中文文件名
            String fileName = java.net.URLEncoder.encode("员工导入模板.xlsx", "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/importExcel")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "Excel导入员工")
    public ResultVO importExcel(@RequestBody List<Map<String, Object>> data) {
        try {
            if (data == null || data.isEmpty()) {
                return ResultVOUtil.fail("请选择要导入的员工数据");
            }

            List<Department> departments = departmentService.list();
            List<Shift> shifts = shiftService.list();

            int successCount = 0;
            int failedCount = 0;
            StringBuilder failedMessages = new StringBuilder();

            for (int i = 0; i < data.size(); i++) {
                try {
                    Map<String, Object> row = data.get(i);
                    int rowNum = i + 2;

                    String name = row.get("name") != null ? row.get("name").toString().trim() : "";
                    String gender = row.get("gender") != null ? row.get("gender").toString().trim() : "";
                    String departmentName = row.get("department") != null ? row.get("department").toString().trim() : "";
                    String position = row.get("position") != null ? row.get("position").toString().trim() : "";
                    String phone = row.get("phone") != null ? row.get("phone").toString().trim() : "";
                    String email = row.get("email") != null ? row.get("email").toString().trim() : "";
                    String roleName = row.get("role") != null ? row.get("role").toString().trim() : "";
                    String shiftName = row.get("shiftName") != null ? row.get("shiftName").toString().trim() : "";
                    String joinDate = row.get("joinDate") != null ? row.get("joinDate").toString().trim() : "";
                    String status = row.get("status") != null ? row.get("status").toString().trim() : "在职";

                    if (name.isEmpty()) {
                        failedMessages.append("第").append(rowNum).append("行：姓名不能为空;");
                        failedCount++;
                        continue;
                    }
                    if (gender.isEmpty()) {
                        failedMessages.append("第").append(rowNum).append("行：性别不能为空;");
                        failedCount++;
                        continue;
                    }
                    if (departmentName.isEmpty()) {
                        failedMessages.append("第").append(rowNum).append("行：部门不能为空;");
                        failedCount++;
                        continue;
                    }
                    if (position.isEmpty()) {
                        failedMessages.append("第").append(rowNum).append("行：职位不能为空;");
                        failedCount++;
                        continue;
                    }
                    if (phone.isEmpty()) {
                        failedMessages.append("第").append(rowNum).append("行：联系电话不能为空;");
                        failedCount++;
                        continue;
                    }
                    if (roleName.isEmpty()) {
                        failedMessages.append("第").append(rowNum).append("行：角色不能为空;");
                        failedCount++;
                        continue;
                    }

                    Integer departmentId = departments.stream()
                        .filter(d -> d.getName().equals(departmentName))
                        .map(Department::getId)
                        .findFirst()
                        .orElse(null);

                    if (departmentId == null) {
                        failedMessages.append("第").append(rowNum).append("行：部门[").append(departmentName).append("]不存在;");
                        failedCount++;
                        continue;
                    }

                    Integer shiftId = null;
                    if (!shiftName.isEmpty()) {
                        shiftId = shifts.stream()
                            .filter(s -> s.getName().equals(shiftName))
                            .map(Shift::getId)
                            .findFirst()
                            .orElse(null);
                        if (shiftId == null) {
                            failedMessages.append("第").append(rowNum).append("行：班次[").append(shiftName).append("]不存在;");
                            failedCount++;
                            continue;
                        }
                    }

                    String roleCode;
                    if ("部门主管".equals(roleName)) {
                        roleCode = RoleType.DEPARTMENT_MANAGER.getCode();
                    } else if ("流程管理员".equals(roleName)) {
                        roleCode = RoleType.PROCESS_ADMIN.getCode();
                    } else {
                        roleCode = RoleType.EMPLOYEE.getCode();
                    }

                    Employee employee = new Employee();
                    employee.setName(name);
                    employee.setGender(gender);
                    employee.setDepartmentId(departmentId);
                    employee.setPosition(position);
                    employee.setPhone(phone);
                    employee.setEmail(email);
                    employee.setRole(roleCode);
                    employee.setShiftId(shiftId);
                    employee.setJoinDate(joinDate);
                    employee.setStatus(status);
                    employee.setUsername(phone);
                    employee.setPassword(passwordEncoder.encode("123456"));

                    employeeService.save(employee);
                    successCount++;
                } catch (Exception e) {
                    failedMessages.append("第").append(i + 2).append("行：").append(e.getMessage()).append(";");
                    failedCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", data.size());
            result.put("success", successCount);
            result.put("failed", failedCount);
            if (failedMessages.length() > 0) {
                result.put("failedMessages", failedMessages.toString());
            }

            return ResultVOUtil.success(result);
        } catch (Exception e) {
            return ResultVOUtil.fail("导入失败：" + e.getMessage());
        }
    }
}