package com.southwind.controller;

import com.southwind.common.UserContext;
import com.southwind.entity.AttendanceOvertime;
import com.southwind.entity.Employee;
import com.southwind.service.AttendanceOvertimeService;
import com.southwind.service.EmployeeService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 加班打卡控制器
 * 独立于正常考勤，专门处理加班打卡
 */
@RestController
@RequestMapping("/overtime")
public class OvertimeController {

    @Autowired
    private AttendanceOvertimeService attendanceOvertimeService;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 加班签到
     * 不受正常考勤约束，一天可以多次加班
     */
    @PostMapping("/checkIn")
    public ResultVO overtimeCheckIn(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        
        try {
            UserContext.UserInfo userInfo = UserContext.getCurrentUser();
            if (userInfo == null) {
                return ResultVOUtil.fail("未登录");
            }

            Integer employeeId = userInfo.getUserId();
            Employee employee = employeeService.getById(employeeId);
            if (employee == null) {
                return ResultVOUtil.fail("员工不存在");
            }

            // 获取当天日期
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 获取当前序号（同一天内的第几条加班记录）
            Integer maxSequence = attendanceOvertimeService.getMaxSequence(employeeId, today);
            Integer sequence = maxSequence + 1;

            // 判断加班类型
            String overtimeType = judgeOvertimeType();

            // 创建加班记录
            AttendanceOvertime overtime = new AttendanceOvertime();
            overtime.setEmployeeId(employeeId);
            overtime.setEmployeeName(employee.getName());
            overtime.setDate(today);
            overtime.setSequence(sequence);
            overtime.setStartTime(LocalDateTime.now());
            overtime.setOvertimeType(overtimeType);
            overtime.setCheckInLatitude(latitude);
            overtime.setCheckInLongitude(longitude);
            overtime.setStatus("待审核");
            overtime.setCreateTime(LocalDateTime.now());
            overtime.setUpdateTime(LocalDateTime.now());

            boolean save = attendanceOvertimeService.save(overtime);
            if (!save) {
                return ResultVOUtil.fail("加班签到失败");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("id", overtime.getId());
            data.put("date", today);
            data.put("sequence", sequence);
            data.put("overtimeType", overtimeType);
            data.put("startTime", overtime.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            ResultVO result = ResultVOUtil.success(data);
            result.setMessage("加班签到成功");
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return ResultVOUtil.fail("操作失败");
        }
    }

    /**
     * 加班签退
     * 完成当前未结束的加班记录
     */
    @PostMapping("/checkOut")
    public ResultVO overtimeCheckOut(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        
        try {
            UserContext.UserInfo userInfo = UserContext.getCurrentUser();
            if (userInfo == null) {
                return ResultVOUtil.fail("未登录");
            }

            Integer employeeId = userInfo.getUserId();

            // 获取当天日期
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 查询当天未结束的加班记录（end_time为空）
            List<AttendanceOvertime> list = attendanceOvertimeService.findByEmployeeAndDate(employeeId, today);
            if (list == null || list.isEmpty()) {
                return ResultVOUtil.fail("今天没有加班记录");
            }

            // 找到最后一条未结束的加班记录
            AttendanceOvertime overtime = null;
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i).getEndTime() == null) {
                    overtime = list.get(i);
                    break;
                }
            }

            if (overtime == null) {
                return ResultVOUtil.fail("没有未结束的加班记录");
            }

            // 设置签退时间
            LocalDateTime endTime = LocalDateTime.now();
            overtime.setEndTime(endTime);
            overtime.setCheckOutLatitude(latitude);
            overtime.setCheckOutLongitude(longitude);
            overtime.setUpdateTime(LocalDateTime.now());

            // 计算加班时长（小时）
            if (overtime.getStartTime() != null) {
                Duration duration = Duration.between(overtime.getStartTime(), endTime);
                double hours = duration.toMinutes() / 60.0;
                overtime.setDuration(Math.round(hours * 100.0) / 100.0);
            }

            boolean update = attendanceOvertimeService.updateById(overtime);
            if (!update) {
                return ResultVOUtil.fail("加班签退失败");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("id", overtime.getId());
            data.put("duration", overtime.getDuration());
            data.put("endTime", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            ResultVO result = ResultVOUtil.success(data);
            result.setMessage("加班签退成功，时长：" + overtime.getDuration() + "小时");
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return ResultVOUtil.fail("操作失败");
        }
    }

    /**
     * 查询当天的加班记录
     */
    @GetMapping("/today")
    public ResultVO getTodayOvertime() {
        try {
            UserContext.UserInfo userInfo = UserContext.getCurrentUser();
            if (userInfo == null) {
                return ResultVOUtil.fail("未登录");
            }

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            List<AttendanceOvertime> list = attendanceOvertimeService.findByEmployeeAndDate(userInfo.getUserId(), today);

            return ResultVOUtil.success(list);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultVOUtil.fail("操作失败");
        }
    }

    /**
     * 查询员工某天的加班记录（公开接口，用于测试）
     */
    @GetMapping("/today/{employeeId}")
    public ResultVO getTodayOvertimeByEmployee(@PathVariable("employeeId") Integer employeeId) {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            List<AttendanceOvertime> list = attendanceOvertimeService.findByEmployeeAndDate(employeeId, today);

            return ResultVOUtil.success(list);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultVOUtil.fail("操作失败");
        }
    }

    /**
     * 查询某时间段的加班记录
     */
    @GetMapping("/list")
    public ResultVO getOvertimeList(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            UserContext.UserInfo userInfo = UserContext.getCurrentUser();
            if (userInfo == null) {
                return ResultVOUtil.fail("未登录");
            }

            List<AttendanceOvertime> list = attendanceOvertimeService.findByEmployeeAndDateRange(
                    userInfo.getUserId(), startDate, endDate);

            return ResultVOUtil.success(list);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultVOUtil.fail("操作失败");
        }
    }

    /**
     * 计算加班总时长
     */
    @GetMapping("/totalDuration")
    public ResultVO getTotalDuration(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            UserContext.UserInfo userInfo = UserContext.getCurrentUser();
            if (userInfo == null) {
                return ResultVOUtil.fail("未登录");
            }

            Double total = attendanceOvertimeService.calculateTotalOvertime(
                    userInfo.getUserId(), startDate, endDate);

            Map<String, Object> data = new HashMap<>();
            data.put("totalDuration", total);

            return ResultVOUtil.success(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultVOUtil.fail("操作失败");
        }
    }

    /**
     * 判断加班类型
     * 根据当前时间判断是班前加班、班后加班还是休息日加班
     */
    private String judgeOvertimeType() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int dayOfWeek = now.getDayOfWeek().getValue(); // 1=周一, 7=周日

        // 休息日加班（周六或周日）
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            return "休息日加班";
        }

        // 班前加班（上班前，假设9点上班）
        if (hour < 9) {
            return "班前加班";
        }

        // 班后加班（下班后，假设18点下班）
        if (hour >= 18) {
            return "班后加班";
        }

        // 默认班后加班
        return "班后加班";
    }
}
