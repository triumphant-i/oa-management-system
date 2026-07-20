package com.southwind.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.southwind.entity.Announcement;
import com.southwind.mapper.AnnouncementMapper;
import com.southwind.service.AnnouncementService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 公告服务实现类
 * </p>
 *
 * @author admin
 * @since 2026-07-15
 */
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

}