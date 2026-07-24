package com.southwind.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.southwind.entity.AttendanceOvertime;

import java.util.List;

/**
 * 加班记录服务接口
 */
public interface AttendanceOvertimeService extends IService<AttendanceOvertime> {

    /**
     * 根据员工ID和日期查询加班记录
     */
    List<AttendanceOvertime> findByEmployeeAndDate(Integer employeeId, String date);

    /**
     * 获取当天最大序号
     */
    Integer getMaxSequence(Integer employeeId, String date);

    /**
     * 查询员工某时间段内的加班记录
     */
    List<AttendanceOvertime> findByEmployeeAndDateRange(Integer employeeId, String startDate, String endDate);

    /**
     * 计算员工某时间段内的总加班时长
     */
    Double calculateTotalOvertime(Integer employeeId, String startDate, String endDate);
}
