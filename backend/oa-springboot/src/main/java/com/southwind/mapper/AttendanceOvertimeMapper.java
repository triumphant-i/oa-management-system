package com.southwind.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.southwind.entity.AttendanceOvertime;
import org.apache.ibatis.annotations.Mapper;

/**
 * 加班记录Mapper
 */
@Mapper
public interface AttendanceOvertimeMapper extends BaseMapper<AttendanceOvertime> {
}
