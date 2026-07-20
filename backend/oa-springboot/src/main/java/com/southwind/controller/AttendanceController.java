package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.entity.Attendance;
import com.southwind.service.AttendanceService;
import com.southwind.service.EmployeeService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(17, 30);

    private static final double COMPANY_LAT = 26.02552;
    private static final double COMPANY_LNG = 119.40617;
    private static final double RANGE_METERS = 500;

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

        Attendance attendance = new Attendance();
        attendance.setEmployeeId(employeeId);
        attendance.setEmployeeName(employeeName);
        attendance.setDate(today);
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setCreateTime(LocalDateTime.now());

        if (params.containsKey("latitude") && params.containsKey("longitude")) {
            Double latitude = Double.parseDouble(params.get("latitude").toString());
            Double longitude = Double.parseDouble(params.get("longitude").toString());
            attendance.setCheckInLatitude(latitude);
            attendance.setCheckInLongitude(longitude);
        }

        if (now.isAfter(WORK_START_TIME)) {
            attendance.setStatus("迟到");
        } else {
            attendance.setStatus("正常");
        }

        boolean save = attendanceService.save(attendance);
        if (!save) return ResultVOUtil.fail("签到失败");
        return ResultVOUtil.success("签到成功");
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
        if (now.isBefore(WORK_END_TIME)) {
            if ("正常".equals(status)) {
                status = "早退";
            } else if ("迟到".equals(status)) {
                status = "迟到/早退";
            }
            updateAttendance.setStatus(status);
        }

        boolean update = attendanceService.updateById(updateAttendance);
        if (!update) return ResultVOUtil.fail("签退失败");
        return ResultVOUtil.success("签退成功");
    }

    @GetMapping("/todayStatus/{employeeId}")
    public ResultVO todayStatus(@PathVariable("employeeId") Integer employeeId) {
        LocalDate today = LocalDate.now();

        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.eq("date", today);
        Attendance attendance = attendanceService.getOne(queryWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("date", today);
        result.put("checkIn", attendance != null && attendance.getCheckInTime() != null);
        result.put("checkOut", attendance != null && attendance.getCheckOutTime() != null);
        result.put("status", attendance != null ? attendance.getStatus() : "未签到");
        result.put("checkInTime", attendance != null && attendance.getCheckInTime() != null ? attendance.getCheckInTime() : null);
        result.put("checkOutTime", attendance != null && attendance.getCheckOutTime() != null ? attendance.getCheckOutTime() : null);

        return ResultVOUtil.success(result);
    }

    @GetMapping("/myRecords/{employeeId}")
    public ResultVO myRecords(@PathVariable("employeeId") Integer employeeId) {
        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("employee_id", employeeId);
        queryWrapper.orderByDesc("date");
        queryWrapper.orderByDesc("check_in_time");
        List<Attendance> list = attendanceService.list(queryWrapper);
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
}