package com.southwind.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southwind.entity.Employee;
import com.southwind.entity.Meeting;
import com.southwind.entity.Message;
import com.southwind.service.EmployeeService;
import com.southwind.service.MeetingService;
import com.southwind.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MeetingTask {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private EmployeeService employeeService;

    @Scheduled(cron = "0 */1 * * * ?")
    public void autoEndExpiredMeetings() {
        LocalDateTime now = LocalDateTime.now();

        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "进行中");
        queryWrapper.lt("end_time", now);

        List<Meeting> expiredMeetings = meetingService.list(queryWrapper);
        for (Meeting meeting : expiredMeetings) {
            meeting.setStatus("已结束");
            meeting.setUpdateTime(now);
            meetingService.updateById(meeting);
            
            sendEndNotification(meeting);
        }

        if (!expiredMeetings.isEmpty()) {
            System.out.println("[MeetingTask] 已自动结束" + expiredMeetings.size() + "个过期会议");
        }
    }

    @Scheduled(cron = "0 */1 * * * ?")
    public void notifyMeetingNearEnd() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tenMinutesLater = now.plusMinutes(10);

        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "进行中");
        queryWrapper.gt("end_time", now);
        queryWrapper.le("end_time", tenMinutesLater);
        // 只查询未发送过提醒的会议（使用nested确保OR在括号内，不破坏AND条件链）
        queryWrapper.and(w -> w.isNull("is_reminded").or().eq("is_reminded", 0));

        List<Meeting> nearEndMeetings = meetingService.list(queryWrapper);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Meeting meeting : nearEndMeetings) {
            String endTimeStr = meeting.getEndTime().format(formatter);
            
            Message organizerMsg = new Message();
            organizerMsg.setSenderId(meeting.getOrganizerId());
            organizerMsg.setSenderName(meeting.getOrganizerName());
            organizerMsg.setReceiverId(meeting.getOrganizerId());
            organizerMsg.setReceiverName(meeting.getOrganizerName());
            organizerMsg.setTitle("会议即将结束");
            organizerMsg.setContent(String.format("您主持的会议【%s】将在10分钟后结束（预计%s），如需延长请及时操作。",
                meeting.getTitle(), endTimeStr));
            organizerMsg.setMsgType("MEETING");
            organizerMsg.setIsRead(0);
            organizerMsg.setIsTop(0);
            messageService.sendMessage(organizerMsg);

            String participantIds = meeting.getParticipantIds();
            if (participantIds != null && !participantIds.isEmpty()) {
                String[] ids = participantIds.split(",");
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
                        message.setTitle("会议即将结束");
                        message.setContent(String.format("您参加的会议【%s】将在10分钟后结束（预计%s）。",
                            meeting.getTitle(), endTimeStr));
                        message.setMsgType("MEETING");
                        message.setIsRead(0);
                        message.setIsTop(0);
                        messageService.sendMessage(message);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
            
            // 标记已发送提醒，避免重复发送
            meeting.setIsReminded(1);
            meeting.setUpdateTime(now);
            meetingService.updateById(meeting);
        }

        if (!nearEndMeetings.isEmpty()) {
            System.out.println("[MeetingTask] 已发送" + nearEndMeetings.size() + "个会议即将结束通知");
        }
    }

    private void sendEndNotification(Meeting meeting) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        
        Message organizerMsg = new Message();
        organizerMsg.setSenderId(meeting.getOrganizerId());
        organizerMsg.setSenderName(meeting.getOrganizerName());
        organizerMsg.setReceiverId(meeting.getOrganizerId());
        organizerMsg.setReceiverName(meeting.getOrganizerName());
        organizerMsg.setTitle("会议已结束");
        organizerMsg.setContent("会议【" + meeting.getTitle() + "】已自动结束，会议室已释放。");
        organizerMsg.setMsgType("MEETING");
        organizerMsg.setIsRead(0);
        organizerMsg.setIsTop(0);
        messageService.sendMessage(organizerMsg);

        String participantIds = meeting.getParticipantIds();
        if (participantIds != null && !participantIds.isEmpty()) {
            String[] ids = participantIds.split(",");
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
                    message.setTitle("会议已结束");
                    message.setContent("会议【" + meeting.getTitle() + "】已结束。");
                    message.setMsgType("MEETING");
                    message.setIsRead(0);
                    message.setIsTop(0);
                    messageService.sendMessage(message);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
    }
}