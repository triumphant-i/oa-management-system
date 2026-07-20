package com.southwind.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.southwind.entity.Attachment;

import java.util.List;

public interface AttachmentService extends IService<Attachment> {

  List<Attachment> findByBusinessId(Integer businessId, String businessType);

  boolean deleteByBusinessId(Integer businessId, String businessType);
}
