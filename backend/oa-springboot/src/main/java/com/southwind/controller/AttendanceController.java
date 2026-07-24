package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.entity.Attendance;
import com.southwind.entity.AttendanceOvertime;
import com.southwind.entity.Employee;
import com.southwind.entity.Shift;
import com.southwind.service.AttendanceOvertimeService;
import com.southwind.service.AttendanceService;
import com.southwind.service.EmployeeService;
import com.southwind.service.ShiftService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private AttendanceOvertimeService attendanceOvertimeService;

    private static final double COMPANY_LAT = 26.02552;
    private static final double COMPANY_LNG = 119.40617;
    private static final double RANGE_METERS = 500;

    private Shift getEmployeeShift(Integer employeeId) {
        Employee employee = employeeService.getById(employeeId);
        if (employee != null && employee.getShiftId() != null) {
            Shift shift = shiftService.getById(employee.getShiftId());
            if (shift != null) {
                return shift;
            }
        }
        return shiftService.getDefaultShift();
    }

    @PostMapping("/checkIn")
    public ResultVO checkIn(@RequestBody Map<String, Object> params) {
        Integer employeeId = Integer.parseInt(params.get("employeeId").toString());
        String employeeName = params.get("employeeName").toString();

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.eq("date", today);
        Attendance existing = attendanceService.getOne(queryWrapper);

        if (existing != null && existing.getCheckInTime() != null) {
            return ResultVOUtil.fail("今日已签到");
        }

        Shift shift = getEmployeeShift(employeeId);

        Attendance attendance;
        if (existing != null) {
            attendance = existing;
        } else {
            attendance = new Attendance();
            attendance.setEmployeeId(employeeId);
            attendance.setEmployeeName(employeeName);
            attendance.setDate(today);
            attendance.setCreateTime(LocalDateTime.now());
        }
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setAttendanceStatus("未处理");

        if (params.containsKey("latitude") && params.containsKey("longitude")) {
            Double latitude = Double.parseDouble(params.get("latitude").toString());
            Double longitude = Double.parseDouble(params.get("longitude").toString());
            attendance.setCheckInLatitude(latitude);
            attendance.setCheckInLongitude(longitude);
        }

        String status = "正常";
        Integer lateMinutes = 0;

        LocalTime workStart = shift.getWorkStart();
        LocalTime graceTime = workStart.plusMinutes(shift.getLateGraceMinutes());

        // 正常考勤状态只有三种：正常、迟到、早退
        if (now.isAfter(graceTime)) {
            status = "迟到";
            lateMinutes = (int) Duration.between(workStart, now).toMinutes();
        }
        // 上班前签到也视为正常（加班记录在加班表中单独处理）

        attendance.setStatus(status);
        attendance.setLateMinutes(lateMinutes);

        boolean save;
        if (existing != null) {
            save = attendanceService.updateById(attendance);
        } else {
            save = attendanceService.save(attendance);
        }
        if (!save) return ResultVOUtil.fail("签到失败");

        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("lateMinutes", lateMinutes);
        result.put("shift", shift);

        return ResultVOUtil.success(result);
    }

    @PostMapping("/checkOut")
    public ResultVO checkOut(@RequestBody Map<String, Object> params) {
        Integer employeeId = Integer.parseInt(params.get("employeeId").toString());

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.eq("date", today);
        Attendance attendance = attendanceService.getOne(queryWrapper);

        if (attendance == null) {
            return ResultVOUtil.fail("请先签到");
        }

        if (attendance.getCheckOutTime() != null) {
            return ResultVOUtil.fail("今日已签退");
        }

        Shift shift = getEmployeeShift(employeeId);

        Attendance updateAttendance = new Attendance();
        updateAttendance.setId(attendance.getId());
        updateAttendance.setCheckOutTime(LocalDateTime.now());
        updateAttendance.setUpdateTime(LocalDateTime.now());

        if (params.containsKey("latitude") && params.containsKey("longitude")) {
            Double latitude = Double.parseDouble(params.get("latitude").toString());
            Double longitude = Double.parseDouble(params.get("longitude").toString());
            updateAttendance.setCheckOutLatitude(latitude);
            updateAttendance.setCheckOutLongitude(longitude);
        }

        String status = attendance.getStatus();
        Integer earlyMinutes = 0;

        LocalTime workEnd = shift.getWorkEnd();
        LocalTime earlyThreshold = workEnd.minusMinutes(shift.getLateGraceMinutes());

        // 正常考勤状态只有三种：正常、迟到、早退
        if (now.isBefore(earlyThreshold)) {
            earlyMinutes = (int) Duration.between(now, workEnd).toMinutes();
            status = "早退";
        }
        // 下班后的加班记录在加班表中单独处理，主考勤表只记录正常考勤状态

        updateAttendance.setStatus(status);
        updateAttendance.setEarlyMinutes(earlyMinutes);

        if (attendance.getCheckInTime() != null) {
            double workHours = calculateWorkHours(attendance.getCheckInTime(), LocalDateTime.now(), shift);
            updateAttendance.setWorkHours(workHours);
        }

        boolean update = attendanceService.updateById(updateAttendance);
        if (!update) return ResultVOUtil.fail("签退失败");

        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("earlyMinutes", earlyMinutes);

        return ResultVOUtil.success(result);
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

    @GetMapping("/todayStatus/{employeeId}")
    public ResultVO todayStatus(@PathVariable("employeeId") Integer employeeId) {
        LocalDate today = LocalDate.now();

        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.eq("date", today);
        Attendance attendance = attendanceService.getOne(queryWrapper);

        Shift shift = getEmployeeShift(employeeId);

        Map<String, Object> result = new HashMap<>();
        result.put("date", today);
        result.put("checkIn", attendance != null && attendance.getCheckInTime() != null);
        result.put("checkOut", attendance != null && attendance.getCheckOutTime() != null);
        result.put("status", attendance != null ? attendance.getStatus() : "未签到");
        result.put("checkInTime", attendance != null && attendance.getCheckInTime() != null ? attendance.getCheckInTime() : null);
        result.put("checkOutTime", attendance != null && attendance.getCheckOutTime() != null ? attendance.getCheckOutTime() : null);
        result.put("shift", shift);
        result.put("lateMinutes", attendance != null ? attendance.getLateMinutes() : 0);
        result.put("earlyMinutes", attendance != null ? attendance.getEarlyMinutes() : 0);
        result.put("overtimeBefore", attendance != null ? attendance.getOvertimeBefore() : 0);
        result.put("overtimeAfter", attendance != null ? attendance.getOvertimeAfter() : 0);
        result.put("workHours", attendance != null ? attendance.getWorkHours() : 0);

        return ResultVOUtil.success(result);
    }

    @GetMapping("/myRecords/{employeeId}")
    public ResultVO myRecords(@PathVariable("employeeId") Integer employeeId) {
        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.orderByDesc("date");
        queryWrapper.orderByDesc("check_in_time");
        List<Attendance> list = attendanceService.list(queryWrapper);
        
        // 将状态统一转换为三种：正常、迟到、早退
        for (Attendance attendance : list) {
            String status = attendance.getStatus();
            if (status == null || status.isEmpty()) {
                attendance.setStatus("正常");
            } else if (status.contains("迟到")) {
                attendance.setStatus("迟到");
            } else if (status.contains("早退")) {
                attendance.setStatus("早退");
            } else if (status.contains("加班")) {
                // 加班状态改为正常（加班记录在加班表中单独处理）
                if (attendance.getLateMinutes() != null && attendance.getLateMinutes() > 0) {
                    attendance.setStatus("迟到");
                } else {
                    attendance.setStatus("正常");
                }
            } else {
                attendance.setStatus("正常");
            }
        }
        
        return ResultVOUtil.success(list);
    }

    @GetMapping("/list/{page}/{size}")
    public ResultVO list(@PathVariable("page") Integer page, @PathVariable("size") Integer size) {
        Page<Attendance> attendancePage = new Page<>(page, size);
        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("date");
        Page<Attendance> resultPage = attendanceService.page(attendancePage, queryWrapper);

        PageVO pageVO = new PageVO();
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setData(resultPage.getRecords());
        return ResultVOUtil.success(pageVO);
    }

    @GetMapping("/findByDate/{date}")
    public ResultVO findByDate(@PathVariable("date") String date) {
        LocalDate queryDate = LocalDate.parse(date);
        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("date", queryDate);
        List<Attendance> list = attendanceService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    @PostMapping("/applyForCorrection")
    public ResultVO applyForCorrection(@RequestBody Map<String, Object> params) {
        Integer employeeId = Integer.parseInt(params.get("employeeId").toString());
        String employeeName = params.get("employeeName").toString();
        String dateStr = params.get("date").toString();
        String timeStr = params.get("time").toString();
        String type = params.get("type").toString();
        String reason = params.get("reason").toString();

        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime checkTime = LocalDateTime.parse(dateStr + "T" + timeStr + ":00");

        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.eq("date", date);
        Attendance existing = attendanceService.getOne(queryWrapper);

        Attendance attendance;
        if (existing != null) {
            attendance = existing;
        } else {
            attendance = new Attendance();
            attendance.setEmployeeId(employeeId);
            attendance.setEmployeeName(employeeName);
            attendance.setDate(date);
        }

        if ("上班签到".equals(type)) {
            attendance.setCheckInTime(checkTime);
        } else if ("下班签退".equals(type)) {
            attendance.setCheckOutTime(checkTime);
        } else {
            attendance.setCheckInTime(checkTime);
        }

        attendance.setStatus("待审核");
        attendance.setAttendanceStatus("待审核");
        attendance.setRemark(type + " - " + reason);
        attendance.setCreateTime(LocalDateTime.now());
        attendance.setUpdateTime(LocalDateTime.now());

        boolean save;
        if (existing != null) {
            save = attendanceService.updateById(attendance);
        } else {
            save = attendanceService.save(attendance);
        }

        if (!save) return ResultVOUtil.fail("申请失败");
        return ResultVOUtil.success("申请已提交");
    }

    @GetMapping("/companyLocation")
    public ResultVO companyLocation() {
        Map<String, Object> result = new HashMap<>();
        result.put("latitude", COMPANY_LAT);
        result.put("longitude", COMPANY_LNG);
        result.put("range", RANGE_METERS);
        result.put("address", "福州物联网产业创新发展中心");
        return ResultVOUtil.success(result);
    }

    @GetMapping("/calculateDistance/{latitude}/{longitude}")
    public ResultVO calculateDistance(@PathVariable("latitude") Double latitude, @PathVariable("longitude") Double longitude) {
        double distance = calculateHaversineDistance(latitude, longitude, COMPANY_LAT, COMPANY_LNG);
        boolean inRange = distance <= RANGE_METERS;

        Map<String, Object> result = new HashMap<>();
        result.put("distance", Math.round(distance));
        result.put("inRange", inRange);
        result.put("range", RANGE_METERS);
        return ResultVOUtil.success(result);
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @GetMapping("/correctionList/{employeeId}")
    public ResultVO correctionList(@PathVariable("employeeId") Integer employeeId) {
        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.eq("status", "待审核");
        queryWrapper.orderByDesc("create_time");
        List<Attendance> list = attendanceService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    @GetMapping("/getEmployeeShift/{employeeId}")
    public ResultVO getEmployeeShiftInfo(@PathVariable("employeeId") Integer employeeId) {
        Shift shift = getEmployeeShift(employeeId);
        return ResultVOUtil.success(shift);
    }

    /**
     * 获取员工某时间段的完整考勤记录（包含加班记录）
     */
    @GetMapping("/myAttendanceWithOvertime/{employeeId}/{startDate}/{endDate}")
    public ResultVO myAttendanceWithOvertime(
            @PathVariable("employeeId") Integer employeeId,
            @PathVariable("startDate") String startDate,
            @PathVariable("endDate") String endDate) {
        
        try {
            // 查询正常考勤记录
            QueryWrapper<Attendance> attendanceWrapper = new QueryWrapper<>();
            attendanceWrapper.eq("employee_id", employeeId);
            attendanceWrapper.between("date", startDate, endDate);
            attendanceWrapper.orderByDesc("date");
            List<Attendance> attendanceList = attendanceService.list(attendanceWrapper);

            // 查询加班记录
            List<AttendanceOvertime> overtimeList = attendanceOvertimeService.findByEmployeeAndDateRange(
                    employeeId, startDate, endDate);

            // 计算总加班时长
            Double totalOvertime = attendanceOvertimeService.calculateTotalOvertime(
                    employeeId, startDate, endDate);

            Map<String, Object> result = new HashMap<>();
            result.put("attendanceList", attendanceList);
            result.put("overtimeList", overtimeList);
            result.put("totalOvertime", totalOvertime);

            return ResultVOUtil.success(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultVOUtil.fail("查询失败");
        }
    }

    /**
     * 获取今日完整考勤信息（包含今日加班记录）
     */
    @GetMapping("/todayFullInfo/{employeeId}")
    public ResultVO todayFullInfo(@PathVariable("employeeId") Integer employeeId) {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 查询今日考勤记录
            QueryWrapper<Attendance> attendanceWrapper = new QueryWrapper<>();
            attendanceWrapper.eq("employee_id", employeeId);
            attendanceWrapper.eq("date", today);
            Attendance attendance = attendanceService.getOne(attendanceWrapper);

            // 查询今日加班记录
            List<AttendanceOvertime> overtimeList = attendanceOvertimeService.findByEmployeeAndDate(
                    employeeId, today);

            Shift shift = getEmployeeShift(employeeId);

            Map<String, Object> result = new HashMap<>();
            result.put("attendance", attendance);
            result.put("overtimeList", overtimeList);
            result.put("shift", shift);

            return ResultVOUtil.success(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultVOUtil.fail("查询失败");
        }
    }
}