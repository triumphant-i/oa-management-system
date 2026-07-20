package com.southwind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.southwind.entity.Meeting;
import com.southwind.entity.MeetingRoom;
import com.southwind.entity.Employee;
import com.southwind.exception.MeetingConflictException;
import com.southwind.mapper.MeetingMapper;
import com.southwind.mapper.MeetingRoomMapper;
import com.southwind.mapper.EmployeeMapper;
import com.southwind.service.MeetingService;
import com.southwind.service.DatabaseProcedureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 会议服务实现类
 * 包含会议室时间冲突检测、冗余字段同步等功能
 */
@Service
public class MeetingServiceImpl extends ServiceImpl<MeetingMapper, Meeting> implements MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingServiceImpl.class);

    @Autowired
    private DatabaseProcedureService databaseProcedureService;

    @Autowired
    private MeetingRoomMapper meetingRoomMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 重写保存方法，添加时间冲突检测
     */
    @Override
    public boolean save(Meeting meeting) {
        // 1. 检查会议室是否存在
        MeetingRoom room = meetingRoomMapper.selectById(meeting.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("会议室不存在");
        }

        // 2. 检查组织者是否存在
        Employee organizer = employeeMapper.selectById(meeting.getOrganizerId());
        if (organizer == null) {
            throw new IllegalArgumentException("组织者不存在");
        }

        // 3. 检查时间合法性
        if (meeting.getEndTime().isBefore(meeting.getStartTime())) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }

        // 4. 使用存储过程检查会议室可用性
        boolean isAvailable = databaseProcedureService.checkRoomAvailability(
                meeting.getRoomId(),
                meeting.getStartTime(),
                meeting.getEndTime()
        );

        if (!isAvailable) {
            // 格式化冲突时间
            String conflictTime = String.format("%s ~ %s",
                    meeting.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    meeting.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );
            throw new MeetingConflictException(Integer.valueOf(room.getId()), room.getName(), conflictTime);
        }

        // 5. 填充冗余字段（便于展示）
        meeting.setRoomName(room.getName());
        meeting.setOrganizerName(organizer.getName());

        // 6. 保存会议（会触发数据库触发器进行二次检查）
        try {
            return super.save(meeting);
        } catch (Exception e) {
            // 捕获数据库触发器抛出的异常
            if (e.getMessage() != null && e.getMessage().contains("时间冲突")) {
                throw new MeetingConflictException(Integer.valueOf(room.getId()), room.getName(),
                        meeting.getStartTime() + " ~ " + meeting.getEndTime());
            }
            throw e;
        }
    }

    /**
     * 重写更新方法，添加时间冲突检测
     */
    @Override
    public boolean updateById(Meeting meeting) {
        // 1. 检查会议室是否存在
        MeetingRoom room = meetingRoomMapper.selectById(meeting.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("会议室不存在");
        }

        // 2. 检查时间合法性
        if (meeting.getEndTime().isBefore(meeting.getStartTime())) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }

        // 3. 检查是否修改了时间或会议室
        Meeting oldMeeting = this.getById(meeting.getId());
        boolean needCheck = !oldMeeting.getRoomId().equals(meeting.getRoomId()) ||
                !oldMeeting.getStartTime().equals(meeting.getStartTime()) ||
                !oldMeeting.getEndTime().equals(meeting.getEndTime());

        if (needCheck) {
            // 4. 使用存储过程检查会议室可用性（排除自己）
            LambdaQueryWrapper<Meeting> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Meeting::getRoomId, meeting.getRoomId())
                    .eq(Meeting::getStatus, "已预约")
                    .ne(Meeting::getId, meeting.getId())
                    .and(wrapper -> wrapper
                            .or(w -> w.between(Meeting::getStartTime, meeting.getStartTime(), meeting.getEndTime()))
                            .or(w -> w.between(Meeting::getEndTime, meeting.getStartTime(), meeting.getEndTime()))
                            .or(w -> w.lt(Meeting::getStartTime, meeting.getStartTime())
                                    .gt(Meeting::getEndTime, meeting.getEndTime()))
                    );

            Integer conflictCount = this.baseMapper.selectCount(queryWrapper).intValue();
            if (conflictCount != null && conflictCount > 0) {
                String conflictTime = String.format("%s ~ %s",
                        meeting.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        meeting.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                );
                throw new MeetingConflictException(Integer.valueOf(room.getId()), room.getName(), conflictTime);
            }
        }

        // 5. 更新冗余字段
        Employee organizer = employeeMapper.selectById(meeting.getOrganizerId());
        if (organizer != null) {
            meeting.setOrganizerName(organizer.getName());
        }
        meeting.setRoomName(room.getName());

        // 6. 更新会议（会触发数据库触发器进行二次检查）
        try {
            return super.updateById(meeting);
        } catch (Exception e) {
            // 捕获数据库触发器抛出的异常
            if (e.getMessage() != null && e.getMessage().contains("时间冲突")) {
                throw new MeetingConflictException(Integer.valueOf(room.getId()), room.getName(),
                        meeting.getStartTime() + " ~ " + meeting.getEndTime());
            }
            throw e;
        }
    }

    /**
     * 取消会议预约
     * 
     * @param meetingId 会议ID
     * @return 是否成功
     */
    public boolean cancelMeeting(Integer meetingId) {
        Meeting meeting = this.getById(meetingId);
        if (meeting == null) {
            throw new IllegalArgumentException("会议不存在");
        }

        meeting.setStatus("已取消");
        return this.updateById(meeting);
    }

    /**
     * 完成会议
     * 
     * @param meetingId 会议ID
     * @return 是否成功
     */
    public boolean completeMeeting(Integer meetingId) {
        Meeting meeting = this.getById(meetingId);
        if (meeting == null) {
            throw new IllegalArgumentException("会议不存在");
        }

        meeting.setStatus("已完成");
        return this.updateById(meeting);
    }
}