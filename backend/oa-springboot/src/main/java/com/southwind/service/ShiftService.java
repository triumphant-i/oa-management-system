package com.southwind.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.southwind.entity.Shift;

import java.util.List;

public interface ShiftService extends IService<Shift> {

    List<Shift> getStandardShifts();

    Shift getDefaultShift();

    Shift getShiftByEmployeeId(Integer employeeId);
}