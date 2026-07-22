package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.entity.Employee;
import com.southwind.entity.Meeting;
import com.southwind.entity.MeetingRoom;
import com.southwind.entity.Message;
import com.southwind.service.EmployeeService;
import com.southwind.service.MeetingRoomService;
import com.southwind.service.MeetingService;
import com.southwind.service.MessageService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 会议预约控制器
 * 新建控制器
 */
@RestController
@RequestMapping("/meeting")
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MeetingRoomService meetingRoomService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 预约会议
     */
    @PostMapping("/book")
    public ResultVO book(@RequestBody Meeting meeting) {
        // 检查会议室是否存在
        MeetingRoom room = meetingRoomService.getById(meeting.getRoomId());
        if (room == null) {
            return ResultVOUtil.fail("会议室不存在");
        }

        // 检查会议室是否可用
        if (!"可用".equals(room.getStatus())) {
            return ResultVOUtil.fail("会议室当前不可用");
        }

        // 检查时间冲突
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", meeting.getRoomId());
        queryWrapper.ne("status", "已取消");

        // 时间冲突检查逻辑
        List<Meeting> existingMeetings = meetingService.list(queryWrapper);
        for (Meeting m : existingMeetings) {
            if (isTimeConflict(meeting.getStartTime(), meeting.getEndTime(),
                    m.getStartTime(), m.getEndTime())) {
                return ResultVOUtil.fail("该时间段会议室已被预约");
            }
        }

        meeting.setStatus("已预约");
        meeting.setRoomName(room.getName());
        meeting.setCreateTime(LocalDateTime.now());
        boolean save = meetingService.save(meeting);
        if (!save) return ResultVOUtil.fail("预约失败");

        // 发送会议通知给所有参会人
        String participantIds = meeting.getParticipantIds();
        if (participantIds != null && !participantIds.isEmpty()) {
            String[] ids = participantIds.split(",");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeStr = meeting.getStartTime().format(formatter) + " - " + meeting.getEndTime().format(formatter);

            for (String id : ids) {
                try {
                    Integer receiverId = Integer.parseInt(id.trim());
                    Employee emp = employeeService.getById(receiverId);
                    String receiverName = emp != null ? emp.getName() : "参会人";
                    Message message = new Message();
                    message.setSenderId(meeting.getOrganizerId());
                    message.setSenderName(meeting.getOrganizerName());
                    message.setReceiverId(receiverId);
                    message.setReceiverName(receiverName);
                    message.setTitle("会议通知");
                    message.setContent(String.format("您被邀请参加会议【%s】，时间：%s，地点：%s。请准时参加。",
                        meeting.getTitle(), timeStr, room.getName()));
                    message.setMsgType("MEETING");
                    message.setIsRead(0);
                    message.setIsTop(0);
                    messageService.sendMessage(message);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        // 发送预约成功通知给组织者
        Message organizerMsg = new Message();
        organizerMsg.setSenderId(meeting.getOrganizerId());
        organizerMsg.setSenderName(meeting.getOrganizerName());
        organizerMsg.setReceiverId(meeting.getOrganizerId());
        organizerMsg.setReceiverName(meeting.getOrganizerName());
        organizerMsg.setTitle("会议预约成功");
        organizerMsg.setContent("会议【" + meeting.getTitle() + "】已成功预约，地点：" + room.getName() + "。");
        organizerMsg.setMsgType("MEETING");
        organizerMsg.setIsRead(0);
        organizerMsg.setIsTop(0);
        messageService.sendMessage(organizerMsg);

        return ResultVOUtil.success("预约成功");
    }

    /**
     * 时间冲突判断
     */
    private boolean isTimeConflict(LocalDateTime start1, LocalDateTime end1,
                                   LocalDateTime start2, LocalDateTime end2) {
        return !(end1.isBefore(start2) || start1.isAfter(end2));
    }

    /**
     * 查询我的会议
     */
    @GetMapping("/myMeetings/{organizerId}")
    public ResultVO myMeetings(@PathVariable("organizerId") Integer organizerId) {
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organizer_id", organizerId);
        queryWrapper.ne("status", "已取消");
        queryWrapper.orderByAsc("start_time");
        List<Meeting> list = meetingService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    /**
     * 分页查询所有会议
     */
    @GetMapping("/list/{page}/{size}")
    public ResultVO list(@PathVariable("page") Integer page, @PathVariable("size") Integer size) {
        Page<Meeting> meetingPage = new Page<>(page, size);
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        Page<Meeting> resultPage = meetingService.page(meetingPage, queryWrapper);

        PageVO pageVO = new PageVO();
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setData(resultPage.getRecords());
        return ResultVOUtil.success(pageVO);
    }

    /**
     * 查询所有会议（管理员用，支持按状态过滤）
     */
    @GetMapping("/all")
    public ResultVO allMeetings(@RequestParam(value = "status", required = false) String status) {
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("start_time");
        List<Meeting> list = meetingService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    /**
     * 查询我需要参加的会议（作为参会人被邀请）
     */
    @GetMapping("/myInvited/{employeeId}")
    public ResultVO myInvitedMeetings(@PathVariable("employeeId") Integer employeeId) {
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("status", "已取消");
        // participant_ids 是逗号分隔的ID字符串，用 FIND_IN_SET 查询
        queryWrapper.apply("FIND_IN_SET({0}, participant_ids)", employeeId);
        queryWrapper.orderByDesc("start_time");
        List<Meeting> list = meetingService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    /**
     * 取消会议
     */
    @PutMapping("/cancel/{id}")
    public ResultVO cancel(@PathVariable("id") Integer id) {
        Meeting meeting = meetingService.getById(id);
        if (meeting == null) {
            return ResultVOUtil.fail("会议不存在");
        }
        meeting.setStatus("已取消");
        meeting.setUpdateTime(LocalDateTime.now());

        boolean update = meetingService.updateById(meeting);
        if (!update) return ResultVOUtil.fail("取消失败");
        return ResultVOUtil.success("取消成功");
    }

    /**
     * 查询会议室的预约情况
     */
    @GetMapping("/roomMeetings/{roomId}")
    public ResultVO roomMeetings(@PathVariable("roomId") Integer roomId) {
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId);
        queryWrapper.ne("status", "已取消");
        queryWrapper.orderByAsc("start_time");
        List<Meeting> list = meetingService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    /**
     * 更新会议信息
     */
    @PutMapping("/update")
    public ResultVO update(@RequestBody Meeting meeting) {
        // 获取原会议记录，确保有完整数据
        Meeting existing = meetingService.getById(meeting.getId());
        if (existing == null) {
            return ResultVOUtil.fail("会议不存在");
        }
        
        // 记录变更内容
        StringBuilder changeDetails = new StringBuilder();
        if (meeting.getTitle() != null && !meeting.getTitle().equals(existing.getTitle())) {
            changeDetails.append(String.format("会议主题变更为「%s」；", meeting.getTitle()));
        }
        if (meeting.getStartTime() != null && !meeting.getStartTime().equals(existing.getStartTime())) {
            changeDetails.append(String.format("开始时间变更为「%s」；", 
                meeting.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }
        if (meeting.getEndTime() != null && !meeting.getEndTime().equals(existing.getEndTime())) {
            changeDetails.append(String.format("结束时间变更为「%s」；", 
                meeting.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        }
        if (meeting.getParticipants() != null && !meeting.getParticipants().equals(existing.getParticipants())) {
            changeDetails.append(String.format("参会人数变更为「%s」；", meeting.getParticipants()));
        }
        if (meeting.getParticipantIds() != null && !meeting.getParticipantIds().equals(existing.getParticipantIds())) {
            changeDetails.append("参会人员有调整；");
        }
        
        // 合并字段：前端提供的字段覆盖原记录
        if (meeting.getTitle() != null) existing.setTitle(meeting.getTitle());
        if (meeting.getStartTime() != null) existing.setStartTime(meeting.getStartTime());
        if (meeting.getEndTime() != null) existing.setEndTime(meeting.getEndTime());
        if (meeting.getParticipants() != null) existing.setParticipants(meeting.getParticipants());
        if (meeting.getParticipantIds() != null) existing.setParticipantIds(meeting.getParticipantIds());
        if (meeting.getStatus() != null) existing.setStatus(meeting.getStatus());
        if (meeting.getRemark() != null) existing.setRemark(meeting.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        
        boolean update = meetingService.updateById(existing);
        if (!update) return ResultVOUtil.fail("更新失败");
        
        // 如果有变更，发送消息通知所有参会人
        if (changeDetails.length() > 0) {
            String participantIds = existing.getParticipantIds();
            if (participantIds != null && !participantIds.isEmpty()) {
                String[] ids = participantIds.split(",");
                for (String id : ids) {
                    try {
                        Integer receiverId = Integer.parseInt(id.trim());
                        Employee emp = employeeService.getById(receiverId);
                        String receiverName = emp != null ? emp.getName() : "参会人";
                        Message message = new Message();
                        message.setSenderId(existing.getOrganizerId());
                        message.setSenderName(existing.getOrganizerName());
                        message.setReceiverId(receiverId);
                        message.setReceiverName(receiverName);
                        message.setTitle("会议信息变更通知");
                        message.setContent(String.format("您参加的会议【%s】信息已更新：%s请关注最新安排。",
                            existing.getTitle(), changeDetails.toString()));
                        message.setMsgType("MEETING");
                        message.setIsRead(0);
                        message.setIsTop(0);
                        messageService.sendMessage(message);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
            
            // 通知组织者
            Message organizerMsg = new Message();
            organizerMsg.setSenderId(existing.getOrganizerId());
            organizerMsg.setSenderName(existing.getOrganizerName());
            organizerMsg.setReceiverId(existing.getOrganizerId());
            organizerMsg.setReceiverName(existing.getOrganizerName());
            organizerMsg.setTitle("会议修改成功");
            organizerMsg.setContent(String.format("会议【%s】已成功修改：%s", 
                existing.getTitle(), changeDetails.toString()));
            organizerMsg.setMsgType("MEETING");
            organizerMsg.setIsRead(0);
            organizerMsg.setIsTop(0);
            messageService.sendMessage(organizerMsg);
        }
        
        return ResultVOUtil.success(null);
    }
    
    /**
     * 删除会议
     */
    @DeleteMapping("/deleteById/{id}")
    public ResultVO deleteById(@PathVariable("id") Integer id) {
        boolean delete = meetingService.removeById(id);
        if (!delete) return ResultVOUtil.fail("删除失败");
        return ResultVOUtil.success(null);
    }

    /**
     * 开始会议
     */
    @PutMapping("/start/{id}")
    public ResultVO startMeeting(@PathVariable("id") Integer id) {
        Meeting meeting = meetingService.getById(id);
        if (meeting == null) {
            return ResultVOUtil.fail("会议不存在");
        }
        if (!"已预约".equals(meeting.getStatus())) {
            return ResultVOUtil.fail("当前会议状态不允许开始");
        }
        
        meeting.setStatus("进行中");
        meeting.setUpdateTime(LocalDateTime.now());
        boolean update = meetingService.updateById(meeting);
        if (!update) return ResultVOUtil.fail("操作失败");
        
        return ResultVOUtil.success("会议已开始");
    }

    /**
     * 结束会议（提前释放）
     */
    @PutMapping("/end/{id}")
    public ResultVO endMeeting(@PathVariable("id") Integer id) {
        Meeting meeting = meetingService.getById(id);
        if (meeting == null) {
            return ResultVOUtil.fail("会议不存在");
        }
        if (!"进行中".equals(meeting.getStatus()) && !"已预约".equals(meeting.getStatus())) {
            return ResultVOUtil.fail("当前会议状态不允许结束");
        }
        
        meeting.setStatus("已结束");
        meeting.setUpdateTime(LocalDateTime.now());
        boolean update = meetingService.updateById(meeting);
        if (!update) return ResultVOUtil.fail("操作失败");
        
        return ResultVOUtil.success("会议已结束");
    }

    /**
     * 延长会议时间
     */
    @PutMapping("/extend/{id}")
    public ResultVO extendMeeting(@PathVariable("id") Integer id, @RequestParam("minutes") Integer minutes) {
        Meeting meeting = meetingService.getById(id);
        if (meeting == null) {
            return ResultVOUtil.fail("会议不存在");
        }
        if (!"进行中".equals(meeting.getStatus())) {
            return ResultVOUtil.fail("当前会议状态不允许延长");
        }
        
        if (minutes <= 0) {
            return ResultVOUtil.fail("延长时间必须大于0");
        }
        
        LocalDateTime newEndTime = meeting.getEndTime().plusMinutes(minutes);
        
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", meeting.getRoomId());
        queryWrapper.ne("id", id);
        queryWrapper.ne("status", "已取消");
        queryWrapper.ne("status", "已结束");
        
        List<Meeting> existingMeetings = meetingService.list(queryWrapper);
        for (Meeting m : existingMeetings) {
            if (isTimeConflict(meeting.getStartTime(), newEndTime, m.getStartTime(), m.getEndTime())) {
                return ResultVOUtil.fail("延长后与其他会议时间冲突");
            }
        }
        
        meeting.setEndTime(newEndTime);
        meeting.setUpdateTime(LocalDateTime.now());
        boolean update = meetingService.updateById(meeting);
        if (!update) return ResultVOUtil.fail("操作失败");
        
        return ResultVOUtil.success("会议已延长" + minutes + "分钟");
    }

    /**
     * 查询当前正在进行的会议
     */
    @GetMapping("/current/{employeeId}")
    public ResultVO getCurrentMeeting(@PathVariable("employeeId") Integer employeeId) {
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organizer_id", employeeId);
        queryWrapper.eq("status", "进行中");
        queryWrapper.orderByDesc("start_time");
        Meeting meeting = meetingService.getOne(queryWrapper);
        return ResultVOUtil.success(meeting);
    }

    /**
     * 查询即将开始的会议（15分钟内）
     */
    @GetMapping("/upcoming/{employeeId}")
    public ResultVO getUpcomingMeeting(@PathVariable("employeeId") Integer employeeId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fifteenMinutesLater = now.plusMinutes(15);
        
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("organizer_id", employeeId);
        queryWrapper.eq("status", "已预约");
        queryWrapper.ge("start_time", now);
        queryWrapper.le("start_time", fifteenMinutesLater);
        queryWrapper.orderByAsc("start_time");
        
        Meeting meeting = meetingService.getOne(queryWrapper);
        return ResultVOUtil.success(meeting);
    }

    /**
     * 检查会议室某天是否全部约满
     */
    @GetMapping("/checkFull/{roomId}/{date}")
    public ResultVO checkRoomFull(@PathVariable("roomId") Integer roomId, @PathVariable("date") String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId);
        queryWrapper.ne("status", "已取消");
        queryWrapper.ge("start_time", dayStart);
        queryWrapper.lt("end_time", dayEnd);
        queryWrapper.orderByAsc("start_time");
        
        List<Meeting> meetings = meetingService.list(queryWrapper);
        
        int totalMinutes = 13 * 60;
        int bookedMinutes = 0;
        
        for (Meeting m : meetings) {
            bookedMinutes += (int) java.time.Duration.between(m.getStartTime(), m.getEndTime()).toMinutes();
        }
        
        boolean isFull = bookedMinutes >= totalMinutes;
        return ResultVOUtil.success(isFull);
    }

    /**
     * 自动结束过期会议（定时任务调用）
     */
    @PostMapping("/autoEnd")
    public ResultVO autoEndExpiredMeetings() {
        LocalDateTime now = LocalDateTime.now();
        
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "进行中");
        queryWrapper.lt("end_time", now);
        
        List<Meeting> expiredMeetings = meetingService.list(queryWrapper);
        for (Meeting meeting : expiredMeetings) {
            meeting.setStatus("已结束");
            meeting.setUpdateTime(now);
            meetingService.updateById(meeting);
        }
        
        return ResultVOUtil.success("已自动结束" + expiredMeetings.size() + "个过期会议");
    }
}