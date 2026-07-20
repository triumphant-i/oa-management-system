package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.annotation.RequirePermission;
import com.southwind.vo.ResultVO;
import com.southwind.common.UserContext;
import com.southwind.entity.Department;
import com.southwind.entity.Employee;
import com.southwind.enums.RoleType;
import com.southwind.service.DepartmentService;
import com.southwind.service.EmployeeService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 部门控制器（增强版 - 支持角色权限校验）
 * 
 * 权限矩阵：
 * - 系统管理员：全部权限（增删改查、层级设置、指定负责人、执行调岗）
 * - 部门主管：只读（本部门信息与成员）；可发起调岗申请
 * - 流程管理员：无权限
 * - 普通员工：无权限（个人中心可只读查看自己所属部门）
 */
@RestController
@RequestMapping("/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 添加部门
     * 权限：仅系统管理员
     */
    @PostMapping("/save")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "添加部门")
    public ResultVO save(@RequestBody Department department) {
        // 设置默认值
        if (department.getParentId() == null) {
            department.setParentId(0);
        }
        if (department.getLevel() == null) {
            // 根据父部门计算层级
            if (department.getParentId() == 0) {
                department.setLevel(1);
            } else {
                Department parentDept = departmentService.getById(department.getParentId());
                if (parentDept != null) {
                    department.setLevel(parentDept.getLevel() + 1);
                }
            }
        }
        
        boolean save = departmentService.save(department);
        if (!save) return ResultVOUtil.fail("添加失败");
        
        // 更新部门路径
        updateDepartmentPath(department.getId(), department.getParentId());
        
        return ResultVOUtil.success(null);
    }

    /**
     * 查询所有部门
     * 权限：系统管理员可以查看所有，部门主管只能查看本部门，普通员工只能查看自己所属部门
     */
    @GetMapping("/list")
    public ResultVO list() {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }
        
        // 系统管理员可以查看所有部门
        if (currentUser.getRole() == RoleType.SYSTEM_ADMIN) {
            List<Department> list = departmentService.list();
            return ResultVOUtil.success(list);
        }
        
        // 部门主管只能查看本部门
        if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
            if (currentUser.getDepartmentId() != null) {
                Department department = departmentService.getById(currentUser.getDepartmentId());
                return ResultVOUtil.success(List.of(department));
            }
        }
        
        // 普通员工只能查看自己所属部门
        if (currentUser.getRole() == RoleType.EMPLOYEE || currentUser.getRole() == RoleType.PROCESS_ADMIN) {
            if (currentUser.getDepartmentId() != null) {
                Department department = departmentService.getById(currentUser.getDepartmentId());
                return ResultVOUtil.success(List.of(department));
            }
        }
        
        return ResultVOUtil.success(List.of());
    }

    /**
     * 分页查询部门
     * 权限：仅系统管理员
     */
    @GetMapping("/list/{page}/{size}")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "分页查询部门")
    public ResultVO listPage(@PathVariable("page") Integer page, @PathVariable("size") Integer size) {
        Page<Department> departmentPage = new Page<>(page, size);
        Page<Department> resultPage = departmentService.page(departmentPage, null);

        PageVO pageVO = new PageVO();
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setData(resultPage.getRecords());
        return ResultVOUtil.success(pageVO);
    }

    /**
     * 根据ID查询部门
     * 权限：系统管理员可以查看所有，其他角色只能查看自己所属部门
     */
    @GetMapping("/findById/{id}")
    public ResultVO findById(@PathVariable("id") Integer id) {
        Department department = departmentService.getById(id);
        if (department == null) {
            return ResultVOUtil.fail("部门不存在");
        }
        
        // 权限校验
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }
        
        // 系统管理员可以查看所有部门
        if (currentUser.getRole() == RoleType.SYSTEM_ADMIN) {
            return ResultVOUtil.success(department);
        }
        
        // 其他角色只能查看自己所属部门
        if (!id.equals(currentUser.getDepartmentId())) {
            return ResultVOUtil.fail("无权查看该部门信息");
        }
        
        return ResultVOUtil.success(department);
    }

    /**
     * 更新部门
     * 权限：仅系统管理员
     */
    @PutMapping("/update")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "更新部门")
    public ResultVO update(@RequestBody Department department) {
        boolean update = departmentService.updateById(department);
        if (!update) return ResultVOUtil.fail("更新失败");
        
        // 如果修改了父部门，需要更新层级和路径
        if (department.getParentId() != null) {
            updateDepartmentPath(department.getId(), department.getParentId());
        }
        
        return ResultVOUtil.success(null);
    }

    /**
     * 删除部门
     * 权限：仅系统管理员
     */
    @DeleteMapping("/deleteById/{id}")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "删除部门")
    public ResultVO deleteById(@PathVariable("id") Integer id) {
        // 检查部门下是否有员工
        QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("department_id", id);
        long count = employeeService.count(queryWrapper);
        if (count > 0) {
            return ResultVOUtil.fail("该部门下还有员工，无法删除");
        }
        
        // 检查是否有子部门
        QueryWrapper<Department> deptQueryWrapper = new QueryWrapper<>();
        deptQueryWrapper.eq("parent_id", id);
        long subDeptCount = departmentService.count(deptQueryWrapper);
        if (subDeptCount > 0) {
            return ResultVOUtil.fail("该部门下还有子部门，无法删除");
        }

        boolean delete = departmentService.removeById(id);
        if (!delete) return ResultVOUtil.fail("删除失败");
        return ResultVOUtil.success(null);
    }

    /**
     * 查询部门员工数量
     * 权限：系统管理员和部门主管可以查看
     */
    @GetMapping("/countEmployees/{id}")
    public ResultVO countEmployees(@PathVariable("id") Integer id) {
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }
        
        // 系统管理员可以查看所有部门
        if (currentUser.getRole() == RoleType.SYSTEM_ADMIN) {
            QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("department_id", id);
            queryWrapper.eq("status", "在职");
            long count = employeeService.count(queryWrapper);
            return ResultVOUtil.success(count);
        }
        
        // 部门主管只能查看本部门
        if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
            if (!id.equals(currentUser.getDepartmentId())) {
                return ResultVOUtil.fail("无权查看其他部门员工数量");
            }
            QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("department_id", id);
            queryWrapper.eq("status", "在职");
            long count = employeeService.count(queryWrapper);
            return ResultVOUtil.success(count);
        }
        
        return ResultVOUtil.fail("无权访问");
    }

    /**
     * 指定部门负责人
     * 权限：仅系统管理员
     */
    @PutMapping("/assignManager")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "指定部门负责人")
    public ResultVO assignManager(@RequestBody Map<String, Object> params) {
        Integer departmentId = (Integer) params.get("departmentId");
        Integer managerId = (Integer) params.get("managerId");
        
        // 检查员工是否存在
        Employee employee = employeeService.getById(managerId);
        if (employee == null) {
            return ResultVOUtil.fail("员工不存在");
        }
        
        // 检查部门是否存在
        Department department = departmentService.getById(departmentId);
        if (department == null) {
            return ResultVOUtil.fail("部门不存在");
        }
        
        // 更新部门负责人
        Department updateDept = new Department();
        updateDept.setId(departmentId);
        updateDept.setManagerId(managerId);
        updateDept.setManagerName(employee.getName());
        updateDept.setManagerPhone(employee.getPhone());
        
        boolean success = departmentService.updateById(updateDept);
        if (success) {
            return ResultVOUtil.success("负责人设置成功");
        } else {
            return ResultVOUtil.fail("负责人设置失败");
        }
    }

    /**
     * 员工调岗
     * 权限：系统管理员可以执行调岗，部门主管可以发起调岗申请
     */
    @PostMapping("/transferEmployee")
    public ResultVO transferEmployee(@RequestBody Map<String, Object> params) {
        Integer employeeId = (Integer) params.get("employeeId");
        Integer newDepartmentId = (Integer) params.get("newDepartmentId");
        
        UserContext.UserInfo currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return ResultVOUtil.fail("未登录");
        }
        
        // 系统管理员可以直接执行调岗
        if (currentUser.getRole() == RoleType.SYSTEM_ADMIN) {
            Employee employee = employeeService.getById(employeeId);
            if (employee == null) {
                return ResultVOUtil.fail("员工不存在");
            }
            
            Employee updateEmployee = new Employee();
            updateEmployee.setId(employeeId);
            updateEmployee.setDepartmentId(newDepartmentId);
            
            boolean success = employeeService.updateById(updateEmployee);
            if (success) {
                return ResultVOUtil.success("调岗成功");
            } else {
                return ResultVOUtil.fail("调岗失败");
            }
        }
        
        // 部门主管可以发起调岗申请（这里简化处理，实际应该走审批流程）
        if (currentUser.getRole() == RoleType.DEPARTMENT_MANAGER) {
            // TODO: 创建调岗申请，走审批流程
            return ResultVOUtil.fail("部门主管调岗需要通过审批流程，功能开发中");
        }
        
        return ResultVOUtil.fail("无权执行调岗操作");
    }

    /**
     * 获取部门树形结构
     * 权限：仅系统管理员
     */
    @GetMapping("/tree")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "获取部门树形结构")
    public ResultVO getDepartmentTree() {
        List<Department> allDepartments = departmentService.list();
        // TODO: 构建树形结构
        return ResultVOUtil.success(allDepartments);
    }

    /**
     * 更新部门路径（私有方法）
     */
    private void updateDepartmentPath(Integer departmentId, Integer parentId) {
        Department department = departmentService.getById(departmentId);
        if (department == null) {
            return;
        }
        
        String path;
        if (parentId == null || parentId == 0) {
            path = String.valueOf(departmentId);
        } else {
            Department parentDept = departmentService.getById(parentId);
            if (parentDept != null && parentDept.getPath() != null) {
                path = parentDept.getPath() + "," + departmentId;
            } else {
                path = String.valueOf(departmentId);
            }
        }
        
        Department updateDept = new Department();
        updateDept.setId(departmentId);
        updateDept.setPath(path);
        departmentService.updateById(updateDept);
    }
}