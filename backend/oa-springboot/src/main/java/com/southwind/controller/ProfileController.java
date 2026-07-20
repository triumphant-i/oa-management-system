package com.southwind.controller;

import com.southwind.constant.SystemConstants;
import com.southwind.entity.Department;
import com.southwind.entity.Employee;
import com.southwind.enums.RoleType;
import com.southwind.service.DepartmentService;
import com.southwind.service.EmployeeService;
import com.southwind.util.PasswordEncoder;
import com.southwind.util.ResultVOUtil;
import com.southwind.util.ValidatorUtil;
import com.southwind.vo.ResultVO;
import com.southwind.common.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

            Map<String, Object> data = new HashMap<>();
            data.put("id", employee.getId());
            data.put("name", employee.getName());
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

            Employee employee = new Employee();
            employee.setId(employeeId);

            if (params.containsKey("name")) {
                employee.setName(params.get("name").toString());
            }
            if (params.containsKey("gender")) {
                employee.setGender(params.get("gender").toString());
            }
            if (params.containsKey("position")) {
                employee.setPosition(params.get("position").toString());
            }
            if (params.containsKey("phone")) {
                employee.setPhone(params.get("phone").toString());
            }
            if (params.containsKey("email")) {
                employee.setEmail(params.get("email").toString());
            }

            boolean success = employeeService.updateById(employee);
            if (success) {
                return ResultVOUtil.success("个人信息更新成功");
            } else {
                return ResultVOUtil.fail(SystemConstants.MSG_OPERATION_FAILED);
            }
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

            boolean success = employeeService.updateById(updateEmployee);
            if (success) {
                return ResultVOUtil.success("密码修改成功");
            } else {
                return ResultVOUtil.fail(SystemConstants.MSG_OPERATION_FAILED);
            }
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        } catch (Exception e) {
            return ResultVOUtil.fail("修改密码失败：" + e.getMessage());
        }
    }
}