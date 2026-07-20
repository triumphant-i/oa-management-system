package com.southwind.controller;

import com.southwind.util.ResultVOUtil;
import com.southwind.service.DatabaseProcedureService;
import com.southwind.vo.ResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库存储过程Controller
 * 提供存储过程调用的REST API接口
 */
@RestController
@RequestMapping("/api/database")
public class DatabaseProcedureController {

    private static final Logger log = LoggerFactory.getLogger(DatabaseProcedureController.class);

    @Autowired
    private DatabaseProcedureService databaseProcedureService;

    /**
     * 检查会议室可用性
     * 
     * @param roomId 会议室ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 可用性检查结果
     */
    @GetMapping("/room/availability")
    public ResultVO checkRoomAvailability(
            @RequestParam Integer roomId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        log.info("检查会议室可用性: roomId={}, startTime={}, endTime={}", roomId, startTime, endTime);

        boolean isAvailable = databaseProcedureService.checkRoomAvailability(roomId, startTime, endTime);
        Integer conflictCount = databaseProcedureService.getRoomConflictCount(roomId, startTime, endTime);

        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("startTime", startTime);
        data.put("endTime", endTime);
        data.put("isAvailable", isAvailable);
        data.put("conflictCount", conflictCount);
        
        return ResultVOUtil.success(data);
    }

    /**
     * 批量同步数据到ES
     * 
     * @param indexName 索引名称
     * @param batchSize 批次大小
     * @return 同步结果
     */
    @PostMapping("/es/sync")
    public ResultVO batchSyncToES(
            @RequestParam String indexName,
            @RequestParam(defaultValue = "100") Integer batchSize) {

        log.info("批量同步ES: indexName={}, batchSize={}", indexName, batchSize);

        List<Map<String, Object>> result = databaseProcedureService.batchSyncToES(indexName, batchSize);

        return ResultVOUtil.success(result);
    }

    /**
     * 获取员工考勤统计
     * 
     * @param employeeId 员工ID
     * @param year 年份
     * @param month 月份
     * @return 考勤统计结果
     */
    @GetMapping("/attendance/statistics")
    public ResultVO getEmployeeAttendanceStatistics(
            @RequestParam Integer employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {

        log.info("获取员工考勤统计: employeeId={}, year={}, month={}", employeeId, year, month);

        List<Map<String, Object>> result = databaseProcedureService.getEmployeeAttendanceStatistics(employeeId, year, month);

        return ResultVOUtil.success(result);
    }

    /**
     * 获取审批统计报表
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 审批统计结果
     */
    @GetMapping("/approval/statistics")
    public ResultVO getApprovalStatisticsReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        log.info("获取审批统计报表: startDate={}, endDate={}", startDate, endDate);

        List<Map<String, Object>> result = databaseProcedureService.getApprovalStatisticsReport(startDate, endDate);

        return ResultVOUtil.success(result);
    }
}