package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southwind.annotation.RequirePermission;
import com.southwind.entity.Role;
import com.southwind.enums.RoleType;
import com.southwind.service.RoleService;
import com.southwind.util.ResultVOUtil;
import com.southwind.util.ValidatorUtil;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping("/list")
    public ResultVO list() {
        List<Role> roles = roleService.list();
        return ResultVOUtil.success(roles);
    }

    @GetMapping("/findById/{id}")
    public ResultVO findById(@PathVariable("id") Integer id) {
        try {
            ValidatorUtil.validId(id, "角色ID");
            Role role = roleService.getById(id);
            if (role == null) {
                return ResultVOUtil.fail("角色不存在");
            }
            return ResultVOUtil.success(role);
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    @PostMapping("/save")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "添加角色")
    public ResultVO save(@RequestBody Role role) {
        try {
            ValidatorUtil.notEmpty(role.getName(), "角色名称");
            
            if (role.getCode() == null || role.getCode().isEmpty()) {
                role.setCode(role.getName().toUpperCase().replace(" ", "_"));
            }
            
            role.setCreateTime(LocalDateTime.now());
            role.setUpdateTime(LocalDateTime.now());
            role.setUserCount(0);
            
            if (role.getIcon() == null) role.setIcon("shield-o");
            if (role.getColor() == null) role.setColor("#6c5ce7");
            
            boolean save = roleService.save(role);
            if (!save) return ResultVOUtil.fail("操作失败");
            return ResultVOUtil.success(null);
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    @PutMapping("/update")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "更新角色")
    public ResultVO update(@RequestBody Role role) {
        try {
            ValidatorUtil.validId(role.getId(), "角色ID");
            
            Role existing = roleService.getById(role.getId());
            if (existing == null) {
                return ResultVOUtil.fail("角色不存在");
            }
            
            role.setUpdateTime(LocalDateTime.now());
            boolean update = roleService.updateById(role);
            if (!update) return ResultVOUtil.fail("操作失败");
            return ResultVOUtil.success(null);
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    @DeleteMapping("/deleteById/{id}")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "删除角色")
    public ResultVO deleteById(@PathVariable("id") Integer id) {
        try {
            ValidatorUtil.validId(id, "角色ID");
            
            Role role = roleService.getById(id);
            if (role == null) {
                return ResultVOUtil.fail("角色不存在");
            }
            
            if ("SYSTEM_ADMIN".equals(role.getCode()) || "DEPARTMENT_MANAGER".equals(role.getCode()) || "EMPLOYEE".equals(role.getCode())) {
                return ResultVOUtil.fail("系统内置角色不能删除");
            }
            
            boolean delete = roleService.removeById(id);
            if (!delete) return ResultVOUtil.fail("操作失败");
            return ResultVOUtil.success(null);
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    @GetMapping("/getAllPermissions")
    public ResultVO getAllPermissions() {
        List<Map<String, String>> permissions = List.of(
            Map.of("label", "用户管理", "value", "用户管理"),
            Map.of("label", "角色管理", "value", "角色管理"),
            Map.of("label", "系统设置", "value", "系统设置"),
            Map.of("label", "日志查看", "value", "日志查看"),
            Map.of("label", "数据字典", "value", "数据字典"),
            Map.of("label", "公告管理", "value", "公告管理"),
            Map.of("label", "会议室管理", "value", "会议室管理"),
            Map.of("label", "审批管理", "value", "审批管理"),
            Map.of("label", "考勤管理", "value", "考勤管理"),
            Map.of("label", "部门管理", "value", "部门管理")
        );
        return ResultVOUtil.success(permissions);
    }
}