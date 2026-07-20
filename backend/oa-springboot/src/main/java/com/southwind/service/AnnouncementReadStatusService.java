package com.southwind.service;

import com.southwind.entity.AnnouncementReadStatus;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AnnouncementReadStatusService extends IService<AnnouncementReadStatus> {
    Integer getReadStatus(Integer announcementId, Integer employeeId);
    
    void markAsRead(Integer announcementId, Integer employeeId);
    
    Integer countUnread(Integer employeeId);
    
    void markAllAsRead(Integer employeeId);
    
    Integer countReadByAnnouncementId(Integer announcementId);
}