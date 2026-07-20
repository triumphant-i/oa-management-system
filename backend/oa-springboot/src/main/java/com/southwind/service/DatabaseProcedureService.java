package com.southwind.service;

import com.southwind.mapper.extension.StoredProceduresMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库存储过程服务
 * 封装存储过程调用，提供业务友好的接口
 */
@Service
public class DatabaseProcedureService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseProcedureService.class);

    @Autowired
    private StoredProceduresMapper storedProceduresMapper;

    /**
     * 检查会议室在指定时间段是否可用
     * 
     * @param roomId 会议室ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return true-可用，false-不可用
     */
    public boolean checkRoomAvailability(Integer roomId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("roomId", roomId);
            params.put("startTime", startTime);
            params.put("endTime", endTime);
            
            // 调用存储过程
            storedProceduresMapper.checkRoomAvailability(params);
            
            // 获取输出参数
            Boolean isAvailable = (Boolean) params.get("isAvailable");
            Integer conflictCount = (Integer) params.get("conflictCount");
            
            log.info("会议室可用性检查: roomId={}, 时间段={} ~ {}, 可用={}, 冲突数={}", 
                    roomId, startTime, endTime, isAvailable, conflictCount);
            
            return isAvailable != null && isAvailable;
        } catch (Exception e) {
            log.error("调用存储过程检查会议室可用性失败", e);
            return false;
        }
    }

    /**
     * 获取会议室冲突数量
     * 
     * @param roomId 会议室ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 冲突数量
     */
    public Integer getRoomConflictCount(Integer roomId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("roomId", roomId);
            params.put("startTime", startTime);
            params.put("endTime", endTime);
            
            storedProceduresMapper.checkRoomAvailability(params);
            
            return (Integer) params.get("conflictCount");
        } catch (Exception e) {
            log.error("获取会议室冲突数量失败", e);
            return -1;
        }
    }

    /**
     * 批量同步数据到Elasticsearch
     * 
     * @param indexName 索引名称
     * @param batchSize 批次大小
     * @return 同步结果统计
     */
    public List<Map<String, Object>> batchSyncToES(String indexName, Integer batchSize) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("indexName", indexName);
            params.put("batchSize", batchSize);
            
            List<Map<String, Object>> result = storedProceduresMapper.batchSyncToES(params);
            
            log.info("批量同步ES完成: indexName={}, batchSize={}", indexName, batchSize);
            return result;
        } catch (Exception e) {
            log.error("批量同步ES失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取员工考勤统计
     * 
     * @param employeeId 员工ID
     * @param year 年份
     * @param month 月份
     * @return 考勤统计结果
     */
    public List<Map<String, Object>> getEmployeeAttendanceStatistics(Integer employeeId, Integer year, Integer month) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("employeeId", employeeId);
            params.put("year", year);
            params.put("month", month);
            
            List<Map<String, Object>> result = storedProceduresMapper.getEmployeeAttendanceStatistics(params);
            
            log.info("获取员工考勤统计: employeeId={}, {}年{}月", employeeId, year, month);
            return result;
        } catch (Exception e) {
            log.error("获取员工考勤统计失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取审批统计报表
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 审批统计结果
     */
    public List<Map<String, Object>> getApprovalStatisticsReport(LocalDate startDate, LocalDate endDate) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startDate", startDate);
            params.put("endDate", endDate);
            
            List<Map<String, Object>> result = storedProceduresMapper.getApprovalStatisticsReport(params);
            
            log.info("获取审批统计报表: {} ~ {}", startDate, endDate);
            return result;
        } catch (Exception e) {
            log.error("获取审批统计报表失败", e);
            return new ArrayList<>();
        }
    }
}