package com.southwind.mapper;

import com.southwind.entity.AnnouncementReadStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AnnouncementReadStatusMapper extends BaseMapper<AnnouncementReadStatus> {

    @Select("SELECT is_read FROM t_announcement_read_status WHERE announcement_id = #{announcementId} AND employee_id = #{employeeId}")
    Integer getReadStatus(@Param("announcementId") Integer announcementId, @Param("employeeId") Integer employeeId);

    @Update("UPDATE t_announcement_read_status SET is_read = 1, read_time = NOW() WHERE announcement_id = #{announcementId} AND employee_id = #{employeeId}")
    int markAsRead(@Param("announcementId") Integer announcementId, @Param("employeeId") Integer employeeId);

    @Select("SELECT COUNT(*) FROM t_announcement_read_status WHERE employee_id = #{employeeId} AND is_read = 0")
    Integer countUnread(@Param("employeeId") Integer employeeId);

    @Update("UPDATE t_announcement_read_status SET is_read = 1, read_time = NOW() WHERE employee_id = #{employeeId} AND is_read = 0")
    int markAllAsRead(@Param("employeeId") Integer employeeId);

    @Select("SELECT announcement_id FROM t_announcement_read_status WHERE employee_id = #{employeeId} AND is_read = 0")
    List<Integer> getUnreadAnnouncementIds(@Param("employeeId") Integer employeeId);

    @Select("SELECT COUNT(*) FROM t_announcement_read_status WHERE announcement_id = #{announcementId} AND is_read = 1")
    Integer countReadByAnnouncementId(@Param("announcementId") Integer announcementId);

    @Select("SELECT COUNT(*) FROM t_announcement_read_status WHERE employee_id = #{employeeId} AND is_read = 1")
    Integer countReadByEmployeeId(@Param("employeeId") Integer employeeId);
}