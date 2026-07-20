package com.southwind.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.southwind.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

  @Select("SELECT COUNT(*) FROM t_message WHERE receiver_id = #{receiverId} AND is_read = 0")
  Integer countUnread(@Param("receiverId") Integer receiverId);

  @Select("SELECT * FROM t_message WHERE receiver_id = #{receiverId} ORDER BY is_top DESC, create_time DESC")
  List<Message> findByReceiverId(@Param("receiverId") Integer receiverId);

  @Select("UPDATE t_message SET is_read = 1, read_time = NOW() WHERE id = #{id}")
  void markAsRead(@Param("id") Integer id);

  @Select("UPDATE t_message SET is_read = 1, read_time = NOW() WHERE receiver_id = #{receiverId}")
  void markAllAsRead(@Param("receiverId") Integer receiverId);
}