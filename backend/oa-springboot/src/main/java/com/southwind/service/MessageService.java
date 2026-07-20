package com.southwind.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.southwind.entity.Message;

import java.util.List;

public interface MessageService extends IService<Message> {

  Integer countUnread(Integer receiverId);

  List<Message> findByReceiverId(Integer receiverId);

  void markAsRead(Integer id);

  void markAllAsRead(Integer receiverId);

  boolean sendMessage(Message message);

  boolean sendBatchMessage(List<Integer> receiverIds, String title, String content, String msgType);
}