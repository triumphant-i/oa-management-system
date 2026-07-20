package com.southwind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.southwind.entity.Attachment;
import com.southwind.mapper.AttachmentMapper;
import com.southwind.service.AttachmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, Attachment> implements AttachmentService {

  @Override
  public List<Attachment> findByBusinessId(Integer businessId, String businessType) {
    return baseMapper.findByBusinessId(businessId, businessType);
  }

  @Override
  public boolean deleteByBusinessId(Integer businessId, String businessType) {
    QueryWrapper<Attachment> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("business_id", businessId);
    queryWrapper.eq("business_type", businessType);
    return remove(queryWrapper);
  }
}
