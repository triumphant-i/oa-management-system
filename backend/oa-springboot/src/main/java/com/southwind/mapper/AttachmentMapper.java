package com.southwind.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.southwind.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {

  @Select("SELECT * FROM t_attachment WHERE business_id = #{businessId} AND business_type = #{businessType} ORDER BY create_time DESC")
  List<Attachment> findByBusinessId(@Param("businessId") Integer businessId, @Param("businessType") String businessType);
}
