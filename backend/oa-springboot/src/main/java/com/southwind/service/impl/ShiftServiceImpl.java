package com.southwind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.southwind.entity.Shift;
import com.southwind.mapper.ShiftMapper;
import com.southwind.service.ShiftService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShiftServiceImpl extends ServiceImpl<ShiftMapper, Shift> implements ShiftService {

    @Override
    public List<Shift> getStandardShifts() {
        QueryWrapper<Shift> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_custom", 0);
        queryWrapper.orderByAsc("id");
        return list(queryWrapper);
    }

    @Override
    public Shift getDefaultShift() {
        QueryWrapper<Shift> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_default", 1);
        return getOne(queryWrapper);
    }

    @Override
    public Shift getShiftByEmployeeId(Integer employeeId) {
        QueryWrapper<Shift> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.eq("is_custom", 1);
        Shift customShift = getOne(queryWrapper);
        if (customShift != null) {
            return customShift;
        }
        return getDefaultShift();
    }
}