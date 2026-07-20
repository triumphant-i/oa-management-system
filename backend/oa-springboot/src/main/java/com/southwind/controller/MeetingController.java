package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.entity.Meeting;
import com.southwind.entity.MeetingRoom;
import com.southwind.entity.Message;
import com.southwind.service.MeetingRoomService;
import com.southwind.service.MeetingService;
import com.southwind.service.MessageService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
                    Message message = new Message();
                    message.setSenderId(meeting.getOrganizerId());
                    message.setSenderName(meeting.getOrganizerName());
                    message.setReceiverId(receiverId);
                    message.setReceiverName("参会人");
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
     * 取消会议
     */
    @PutMapping("/cancel/{id}")
    public ResultVO cancel(@PathVariable("id") Integer id) {
        Meeting meeting = new Meeting();
        meeting.setId(id);
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
        meeting.setUpdateTime(LocalDateTime.now());
        boolean update = meetingService.updateById(meeting);
        if (!update) return ResultVOUtil.fail("更新失败");
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
}