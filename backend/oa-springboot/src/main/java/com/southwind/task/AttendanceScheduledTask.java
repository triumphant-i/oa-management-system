package com.southwind.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southwind.entity.Attendance;
import com.southwind.entity.Employee;
import com.southwind.entity.Shift;
import com.southwind.service.AttendanceService;
import com.southwind.service.EmployeeService;
import com.southwind.service.ShiftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class AttendanceScheduledTask {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ShiftService shiftService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void calculateDailyAttendance() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        calculateAttendanceForDate(yesterday);
    }

    @Scheduled(cron = "0 30 23 * * ?")
    public void calculateTodayAttendanceEarly() {
        LocalDate today = LocalDate.now();
        calculateAttendanceForDate(today);
    }

    public void calculateAttendanceForDate(LocalDate date) {
        QueryWrapper<Employee> employeeQuery = new QueryWrapper<>();
        employeeQuery.eq("status", "在职");
        List<Employee> employees = employeeService.list(employeeQuery);

        for (Employee employee : employees) {
            calculateEmployeeAttendance(employee, date);
        }
    }

    private void calculateEmployeeAttendance(Employee employee, LocalDate date) {
        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employee.getId());
        queryWrapper.eq("date", date);
        Attendance attendance = attendanceService.getOne(queryWrapper);

        if (attendance == null) {
            attendance = new Attendance();
            attendance.setEmployeeId(employee.getId());
            attendance.setEmployeeName(employee.getName());
            attendance.setDate(date);
            attendance.setStatus("缺勤");
            attendance.setAttendanceStatus("ABSENT");
            attendance.setLateMinutes(0);
            attendance.setEarlyMinutes(0);
            attendance.setOvertimeBefore(0.0);
            attendance.setOvertimeAfter(0.0);
            attendance.setWorkHours(0.0);
            attendance.setCreateTime(LocalDateTime.now());
            attendance.setUpdateTime(LocalDateTime.now());
            attendanceService.save(attendance);
            return;
        }

        Shift shift = getEmployeeShift(employee);

        String attendanceStatus = "NORMAL";
        Integer lateMinutes = 0;
        Integer earlyMinutes = 0;
        Double overtimeBefore = 0.0;
        Double overtimeAfter = 0.0;
        Double workHours = 0.0;

        LocalDateTime checkInTime = attendance.getCheckInTime();
        LocalDateTime checkOutTime = attendance.getCheckOutTime();

        if (checkInTime == null) {
            attendanceStatus = "ABSENT";
            attendance.setStatus("缺勤");
        } else {
            LocalTime workStart = shift.getWorkStart();
            LocalTime graceTime = workStart.plusMinutes(shift.getLateGraceMinutes());
            LocalTime checkInTimeOnly = checkInTime.toLocalTime();

            if (checkInTimeOnly.isAfter(graceTime)) {
                lateMinutes = (int) Duration.between(workStart, checkInTimeOnly).toMinutes();
                if (lateMinutes >= shift.getLateThresholdMinutes()) {
                    attendanceStatus = "ABSENT";
                    attendance.setStatus("缺勤");
                } else {
                    attendanceStatus = "LATE";
                    attendance.setStatus("迟到");
                }
            }

            if (checkInTimeOnly.isBefore(workStart)) {
                overtimeBefore = Duration.between(checkInTimeOnly, workStart).toMinutes() / 60.0;
                if (overtimeBefore < shift.getOvertimeThresholdMinutes() / 60.0) {
                    overtimeBefore = 0.0;
                }
            }

            if (checkOutTime != null) {
                LocalTime workEnd = shift.getWorkEnd();
                LocalTime earlyThreshold = workEnd.minusMinutes(shift.getLateGraceMinutes());
                LocalTime checkOutTimeOnly = checkOutTime.toLocalTime();

                if (checkOutTimeOnly.isBefore(earlyThreshold)) {
                    earlyMinutes = (int) Duration.between(checkOutTimeOnly, workEnd).toMinutes();
                    if ("LATE".equals(attendanceStatus)) {
                        attendanceStatus = "LATE_EARLY";
                        attendance.setStatus("迟到/早退");
                    } else {
                        attendanceStatus = "EARLY";
                        attendance.setStatus("早退");
                    }
                }

                if (checkOutTimeOnly.isAfter(workEnd)) {
                    overtimeAfter = Duration.between(workEnd, checkOutTimeOnly).toMinutes() / 60.0;
                    if (overtimeAfter < shift.getOvertimeThresholdMinutes() / 60.0) {
                        overtimeAfter = 0.0;
                    }
                }

                workHours = calculateWorkHours(checkInTime, checkOutTime, shift);

                if (overtimeBefore > 0 || overtimeAfter > 0) {
                    if (!"ABSENT".equals(attendanceStatus)) {
                        attendanceStatus = attendanceStatus + "_OVERTIME";
                    }
                }
            } else {
                attendanceStatus = "MISS_CHECKOUT";
                attendance.setStatus("缺签退");
            }
        }

        attendance.setLateMinutes(lateMinutes);
        attendance.setEarlyMinutes(earlyMinutes);
        attendance.setOvertimeBefore(overtimeBefore);
        attendance.setOvertimeAfter(overtimeAfter);
        attendance.setWorkHours(workHours);
        attendance.setAttendanceStatus(attendanceStatus);
        attendance.setUpdateTime(LocalDateTime.now());

        attendanceService.updateById(attendance);
    }

    private Shift getEmployeeShift(Employee employee) {
        if (employee.getShiftId() != null) {
            Shift shift = shiftService.getById(employee.getShiftId());
            if (shift != null) {
                return shift;
            }
        }
        return shiftService.getDefaultShift();
    }

    private double calculateWorkHours(LocalDateTime checkIn, LocalDateTime checkOut, Shift shift) {
        Duration totalDuration = Duration.between(checkIn, checkOut);
        double totalMinutes = totalDuration.toMinutes();

        LocalTime lunchStart = shift.getLunchStart();
        LocalTime lunchEnd = shift.getLunchEnd();
        int lunchDeduct = shift.getLunchDeductMinutes();

        if (lunchStart != null && lunchEnd != null && checkIn.toLocalTime().isBefore(lunchEnd) && checkOut.toLocalTime().isAfter(lunchStart)) {
            totalMinutes -= lunchDeduct;
        }

        return Math.max(0, totalMinutes / 60.0);
    }

    public void recalculateAttendance(Integer attendanceId) {
        Attendance attendance = attendanceService.getById(attendanceId);
        if (attendance == null) {
            return;
        }
        Employee employee = employeeService.getById(attendance.getEmployeeId());
        if (employee != null) {
            calculateEmployeeAttendance(employee, attendance.getDate());
        }
    }
}