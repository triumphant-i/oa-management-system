package com.southwind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.southwind.entity.Announcement;
import com.southwind.entity.AnnouncementReadStatus;
import com.southwind.mapper.AnnouncementReadStatusMapper;
import com.southwind.service.AnnouncementReadStatusService;
import com.southwind.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnnouncementReadStatusServiceImpl extends ServiceImpl<AnnouncementReadStatusMapper, AnnouncementReadStatus> implements AnnouncementReadStatusService {

    @Autowired
    private AnnouncementService announcementService;

    @Override
    public Integer getReadStatus(Integer announcementId, Integer employeeId) {
        Integer status = baseMapper.getReadStatus(announcementId, employeeId);
        return status != null ? status : 0;
    }

    @Override
    @Transactional
    public void markAsRead(Integer announcementId, Integer employeeId) {
        Integer status = baseMapper.getReadStatus(announcementId, employeeId);
        if (status != null && status == 1) {
            return;
        }
        
        if (status != null) {
            baseMapper.markAsRead(announcementId, employeeId);
        } else {
            AnnouncementReadStatus readStatus = new AnnouncementReadStatus();
            readStatus.setAnnouncementId(announcementId);
            readStatus.setEmployeeId(employeeId);
            readStatus.setIsRead(1);
            readStatus.setReadTime(LocalDateTime.now());
            readStatus.setCreateTime(LocalDateTime.now());
            save(readStatus);
        }
    }

    @Override
    public Integer countUnread(Integer employeeId) {
        QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "已发布");
        Integer totalCount = announcementService.count(queryWrapper);
        if (totalCount == null) {
            totalCount = 0;
        }
        
        Integer readCount = baseMapper.countReadByEmployeeId(employeeId);
        if (readCount == null) {
            readCount = 0;
        }
        
        return totalCount - readCount;
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer employeeId) {
        baseMapper.markAllAsRead(employeeId);
    }

    @Override
    public Integer countReadByAnnouncementId(Integer announcementId) {
        return baseMapper.countReadByAnnouncementId(announcementId);
    }
}