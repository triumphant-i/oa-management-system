package com.southwind.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.southwind.entity.Message;
import com.southwind.mapper.MessageMapper;
import com.southwind.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

  @Override
  public Integer countUnread(Integer receiverId) {
    return baseMapper.countUnread(receiverId);
  }

  @Override
  public List<Message> findByReceiverId(Integer receiverId) {
    return baseMapper.findByReceiverId(receiverId);
  }

  @Override
  @Transactional
  public void markAsRead(Integer id) {
    baseMapper.markAsRead(id);
  }

  @Override
  @Transactional
  public void markAllAsRead(Integer receiverId) {
    baseMapper.markAllAsRead(receiverId);
  }

  @Override
  @Transactional
  public boolean sendMessage(Message message) {
    if (message.getIsRead() == null) {
      message.setIsRead(0);
    }
    if (message.getIsTop() == null) {
      message.setIsTop(0);
    }
    if (message.getStatus() == null) {
      message.setStatus("正常");
    }
    message.setCreateTime(LocalDateTime.now());
    return save(message);
  }

  @Override
  @Transactional
  public boolean sendBatchMessage(List<Integer> receiverIds, String title, String content, String msgType) {
    List<Message> messages = new ArrayList<>();
    for (Integer receiverId : receiverIds) {
      Message message = new Message();
      message.setReceiverId(receiverId);
      message.setTitle(title);
      message.setContent(content);
      message.setMsgType(msgType);
      message.setIsRead(0);
      message.setIsTop(0);
      message.setStatus("正常");
      message.setCreateTime(LocalDateTime.now());
      messages.add(message);
    }
    return saveBatch(messages);
  }
}