package com.southwind.mapper.extension;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.StatementType;
import java.util.List;
import java.util.Map;

/**
 * 存储过程Mapper
 * 提供数据库存储过程的调用接口
 */
@Mapper
public interface StoredProceduresMapper {

    /**
     * 检查会议室可用性
     * 调用存储过程：sp_check_room_availability
     * 
     * @param roomId 会议室ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 可用性检查结果
     */
    @Select(value = "{CALL sp_check_room_availability(" +
            "#{roomId, mode=IN, jdbcType=INTEGER}, " +
            "#{startTime, mode=IN, jdbcType=TIMESTAMP}, " +
            "#{endTime, mode=IN, jdbcType=TIMESTAMP}, " +
            "#{isAvailable, mode=OUT, jdbcType=BOOLEAN}, " +
            "#{conflictCount, mode=OUT, jdbcType=INTEGER}" +
            ")}")
    @Options(statementType = StatementType.CALLABLE)
    void checkRoomAvailability(Map<String, Object> params);

    /**
     * 批量同步ES数据
     * 调用存储过程：sp_batch_sync_to_es
     * 
     * @param indexName 索引名称
     * @param batchSize 批次大小
     * @return 同步结果
     */
    @Select(value = "{CALL sp_batch_sync_to_es(" +
            "#{indexName, mode=IN, jdbcType=VARCHAR}, " +
            "#{batchSize, mode=IN, jdbcType=INTEGER}" +
            ")}")
    @Options(statementType = StatementType.CALLABLE)
    List<Map<String, Object>> batchSyncToES(Map<String, Object> params);

    /**
     * 员工考勤统计
     * 调用存储过程：sp_employee_attendance_statistics
     * 
     * @param employeeId 员工ID
     * @param year 年份
     * @param month 月份
     * @return 考勤统计结果
     */
    @Select(value = "{CALL sp_employee_attendance_statistics(" +
            "#{employeeId, mode=IN, jdbcType=INTEGER}, " +
            "#{year, mode=IN, jdbcType=INTEGER}, " +
            "#{month, mode=IN, jdbcType=INTEGER}" +
            ")}")
    @Options(statementType = StatementType.CALLABLE)
    List<Map<String, Object>> getEmployeeAttendanceStatistics(Map<String, Object> params);

    /**
     * 审批统计报表
     * 调用存储过程：sp_approval_statistics_report
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 审批统计结果
     */
    @Select(value = "{CALL sp_approval_statistics_report(" +
            "#{startDate, mode=IN, jdbcType=DATE}, " +
            "#{endDate, mode=IN, jdbcType=DATE}" +
            ")}")
    @Options(statementType = StatementType.CALLABLE)
    List<Map<String, Object>> getApprovalStatisticsReport(Map<String, Object> params);
}