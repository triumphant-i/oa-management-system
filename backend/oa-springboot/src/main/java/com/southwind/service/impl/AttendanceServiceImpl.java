package com.southwind.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.southwind.entity.Attendance;
import com.southwind.mapper.AttendanceMapper;
import com.southwind.service.AttendanceService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 考勤服务实现类
 * </p>
 *
 * @author admin
 * @since 2026-07-15
 */
@Service
public class AttendanceServiceImpl extends ServiceImpl<AttendanceMapper, Attendance> implements AttendanceService {

}