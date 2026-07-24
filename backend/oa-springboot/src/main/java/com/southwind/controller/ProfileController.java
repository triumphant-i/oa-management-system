package com.southwind.controller;

import com.southwind.constant.SystemConstants;
import com.southwind.entity.Department;
import com.southwind.entity.Employee;
import com.southwind.entity.Shift;
import com.southwind.enums.RoleType;
import com.southwind.service.DepartmentService;
import com.southwind.service.EmployeeService;
import com.southwind.service.ShiftService;
import com.southwind.util.CustomPasswordEncoder;
import com.southwind.util.ResultVOUtil;
import com.southwind.util.ValidatorUtil;
import com.southwind.vo.ResultVO;
import com.southwind.common.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private CustomPasswordEncoder passwordEncoder;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @GetMapping("/myInfo")
    public ResultVO getMyInfo() {
        try {
            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            Employee employee = employeeService.getById(currentUser.getUserId());
            if (employee == null) {
                return ResultVOUtil.fail("员工信息不存在");
            }

            String departmentName = "";
            if (employee.getDepartmentId() != null) {
                Department department = departmentService.getById(employee.getDepartmentId());
                if (department != null) {
                    departmentName = department.getName();
                }
            }

            // 获取班次信息
            String shiftName = "";
            String workStart = "";
            String workEnd = "";
            if (employee.getShiftId() != null) {
                Shift shift = shiftService.getById(employee.getShiftId());
                if (shift != null) {
                    shiftName = shift.getName();
                    if (shift.getWorkStart() != null) {
                        workStart = shift.getWorkStart().toString();
                    }
                    if (shift.getWorkEnd() != null) {
                        workEnd = shift.getWorkEnd().toString();
                    }
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("id", employee.getId());
            data.put("name", employee.getName());
            data.put("username", employee.getUsername());
            data.put("gender", employee.getGender());
            data.put("employeeId", employee.getId());
            data.put("department", departmentName);
            data.put("departmentId", employee.getDepartmentId());
            data.put("position", employee.getPosition());
            data.put("joinDate", employee.getJoinDate());
            data.put("phone", employee.getPhone());
            data.put("email", employee.getEmail());
            data.put("avatar", employee.getAvatar());
            data.put("role", employee.getRole());
            data.put("shiftId", employee.getShiftId());
            data.put("shiftName", shiftName);
            data.put("workStart", workStart);
            data.put("workEnd", workEnd);

            return ResultVOUtil.success(data);
        } catch (Exception e) {
            return ResultVOUtil.fail("获取个人信息失败：" + e.getMessage());
        }
    }

    @PutMapping("/update")
    public ResultVO updateProfile(@RequestBody Map<String, Object> params) {
        try {
            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            Integer employeeId = currentUser.getUserId();
            
            // 获取数据库中的员工信息（用于乐观锁校验）
            Employee dbEmployee = employeeService.getById(employeeId);
            if (dbEmployee == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_EMPLOYEE_NOT_FOUND);
            }

            // 检查乐观锁版本
            if (params.containsKey("version")) {
                Integer version = Integer.parseInt(params.get("version").toString());
                if (!version.equals(dbEmployee.getVersion())) {
                    return ResultVOUtil.fail("数据已被其他用户修改，请刷新后重试");
                }
            }

            Employee employee = new Employee();
            employee.setId(employeeId);
            employee.setVersion(dbEmployee.getVersion());

            if (params.containsKey("name")) {
                String name = params.get("name").toString();
                ValidatorUtil.notEmpty(name, "姓名");
                ValidatorUtil.maxLength(name, 50, "姓名");
                employee.setName(name);
            }
            if (params.containsKey("username")) {
                String username = params.get("username").toString();
                ValidatorUtil.notEmpty(username, "账号名");
                ValidatorUtil.maxLength(username, 50, "账号名");
                // 检查username唯一性（排除自身）
                QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("username", username);
                queryWrapper.ne("id", employeeId);
                if (employeeService.getOne(queryWrapper) != null) {
                    return ResultVOUtil.fail("账号名已存在");
                }
                employee.setUsername(username);
            }
            if (params.containsKey("gender")) {
                String gender = params.get("gender").toString();
                if (!"男".equals(gender) && !"女".equals(gender)) {
                    return ResultVOUtil.fail("性别只能是男或女");
                }
                employee.setGender(gender);
            }
            if (params.containsKey("position")) {
                String position = params.get("position").toString();
                ValidatorUtil.maxLength(position, 50, "职位");
                employee.setPosition(position);
            }
            if (params.containsKey("phone")) {
                String phone = params.get("phone").toString();
                ValidatorUtil.notEmpty(phone, "联系电话");
                ValidatorUtil.validatePhone(phone);
                employee.setPhone(phone);
            }
            if (params.containsKey("email")) {
                String email = params.get("email").toString();
                ValidatorUtil.notEmpty(email, "邮箱");
                ValidatorUtil.validateEmail(email);
                employee.setEmail(email);
            }

            boolean success = employeeService.updateById(employee);
            if (!success) {
                return ResultVOUtil.fail("数据已被其他用户修改，请刷新后重试");
            }
            return ResultVOUtil.success("个人信息更新成功");
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        } catch (Exception e) {
            return ResultVOUtil.fail("更新个人信息失败：" + e.getMessage());
        }
    }

    @PutMapping("/updatePassword")
    public ResultVO updatePassword(@RequestBody Map<String, String> params) {
        try {
            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            Integer employeeId = currentUser.getUserId();
            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");

            ValidatorUtil.notEmpty(oldPassword, "原密码");
            ValidatorUtil.notEmpty(newPassword, "新密码");
            ValidatorUtil.minLength(newPassword, 6, "新密码");

            Employee employee = employeeService.getById(employeeId);
            if (employee == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_EMPLOYEE_NOT_FOUND);
            }

            if (!passwordEncoder.matches(oldPassword, employee.getPassword())) {
                return ResultVOUtil.fail("原密码错误");
            }

            Employee updateEmployee = new Employee();
            updateEmployee.setId(employeeId);
            updateEmployee.setPassword(passwordEncoder.encode(newPassword));
            updateEmployee.setVersion(employee.getVersion());

            boolean success = employeeService.updateById(updateEmployee);
            if (!success) {
                return ResultVOUtil.fail("数据已被其他用户修改，请刷新后重试");
            }
            return ResultVOUtil.success("密码修改成功");
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        } catch (Exception e) {
            return ResultVOUtil.fail("修改密码失败：" + e.getMessage());
        }
    }

    @PostMapping("/uploadAvatar")
    public ResultVO uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            UserContext.UserInfo currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                return ResultVOUtil.fail(SystemConstants.MSG_NOT_LOGIN);
            }

            if (file.isEmpty()) {
                return ResultVOUtil.fail("请选择图片");
            }

            // 文件大小限制（5MB）
            long maxSize = 5 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResultVOUtil.fail("图片大小不能超过5MB");
            }

            String fileName = file.getOriginalFilename();
            String extension = "";
            if (fileName != null && fileName.contains(".")) {
                extension = fileName.substring(fileName.lastIndexOf("."));
            }

            if (!extension.toLowerCase().matches("\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                return ResultVOUtil.fail("只支持JPG、JPEG、PNG、GIF、BMP、WEBP格式的图片");
            }

            String uuid = UUID.randomUUID().toString().replace("-", "");
            String newFileName = uuid + extension;
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String filePath = datePath + "/" + newFileName;

            Path path = Paths.get(uploadPath, datePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            File dest = new File(uploadPath + "/" + filePath);
            file.transferTo(dest);

            String avatarUrl = "/uploads/" + filePath;

            // 获取原员工信息，删除旧头像文件
            Employee dbEmployee = employeeService.getById(currentUser.getUserId());
            if (dbEmployee != null && dbEmployee.getAvatar() != null && !dbEmployee.getAvatar().isEmpty()) {
                String oldAvatarPath = dbEmployee.getAvatar();
                if (oldAvatarPath.startsWith("/uploads/")) {
                    // 去掉 /uploads/ 前缀，获取真实路径
                    String realPath = oldAvatarPath.substring("/uploads/".length());
                    File oldFile = new File(uploadPath + "/" + realPath);
                    if (oldFile.exists()) {
                        boolean deleted = oldFile.delete();
                        if (!deleted) {
                            System.out.println("[ProfileController] 旧头像文件删除失败: " + oldFile.getAbsolutePath());
                        }
                    }
                }
            }

            Employee employee = new Employee();
            employee.setId(currentUser.getUserId());
            employee.setAvatar(avatarUrl);
            if (dbEmployee != null) {
                employee.setVersion(dbEmployee.getVersion());
            }

            boolean success = employeeService.updateById(employee);
            if (!success) {
                return ResultVOUtil.fail("数据已被其他用户修改，请刷新后重试");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("avatar", avatarUrl);
            return ResultVOUtil.success(result);
        } catch (IOException e) {
            e.printStackTrace();
            return ResultVOUtil.fail("上传失败");
        } catch (Exception e) {
            return ResultVOUtil.fail("上传失败：" + e.getMessage());
        }
    }
}