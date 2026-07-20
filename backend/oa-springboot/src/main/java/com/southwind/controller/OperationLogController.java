package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southwind.annotation.RequirePermission;
import com.southwind.entity.OperationLog;
import com.southwind.enums.RoleType;
import com.southwind.service.OperationLogService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/operationLog")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping("/list")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "查看操作日志")
    public ResultVO list() {
        QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        List<OperationLog> logs = operationLogService.list(queryWrapper);
        return ResultVOUtil.success(logs);
    }

    @PostMapping("/save")
    public ResultVO save(@RequestBody OperationLog log) {
        log.setCreateTime(LocalDateTime.now());
        boolean save = operationLogService.save(log);
        if (!save) return ResultVOUtil.fail("操作失败");
        return ResultVOUtil.success(null);
    }

    @DeleteMapping("/clear")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "清空操作日志")
    public ResultVO clear() {
        operationLogService.remove(new QueryWrapper<>());
        return ResultVOUtil.success("日志已清空");
    }

    @GetMapping("/countToday")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "今日操作日志数量")
    public ResultVO countToday() {
        QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("create_time", LocalDateTime.now().toLocalDate().atStartOfDay());
        long count = operationLogService.count(queryWrapper);
        return ResultVOUtil.success(count);
    }
}