package com.southwind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.southwind.entity.AttendanceOvertime;
import com.southwind.mapper.AttendanceOvertimeMapper;
import com.southwind.service.AttendanceOvertimeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 加班记录服务实现
 */
@Service
public class AttendanceOvertimeServiceImpl extends ServiceImpl<AttendanceOvertimeMapper, AttendanceOvertime> implements AttendanceOvertimeService {

    @Override
    public List<AttendanceOvertime> findByEmployeeAndDate(Integer employeeId, String date) {
        QueryWrapper<AttendanceOvertime> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.eq("date", date);
        queryWrapper.orderByAsc("sequence");
        return this.list(queryWrapper);
    }

    @Override
    public Integer getMaxSequence(Integer employeeId, String date) {
        // 先查询当天所有记录
        QueryWrapper<AttendanceOvertime> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.eq("date", date);
        queryWrapper.orderByDesc("sequence");
        List<AttendanceOvertime> list = this.list(queryWrapper);
        
        // 如果有记录，返回最大序号
        if (list != null && !list.isEmpty()) {
            AttendanceOvertime item = list.get(0);
            if (item != null && item.getSequence() != null) {
                return item.getSequence();
            }
        }
        return 0;
    }

    @Override
    public List<AttendanceOvertime> findByEmployeeAndDateRange(Integer employeeId, String startDate, String endDate) {
        QueryWrapper<AttendanceOvertime> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.between("date", startDate, endDate);
        queryWrapper.orderByAsc("date", "sequence");
        return this.list(queryWrapper);
    }

    @Override
    public Double calculateTotalOvertime(Integer employeeId, String startDate, String endDate) {
        List<AttendanceOvertime> list = findByEmployeeAndDateRange(employeeId, startDate, endDate);
        if (list == null || list.isEmpty()) {
            return 0.0;
        }
        return list.stream()
                .filter(o -> "已通过".equals(o.getStatus()))
                .mapToDouble(o -> o.getDuration() != null ? o.getDuration() : 0)
                .sum();
    }
}
