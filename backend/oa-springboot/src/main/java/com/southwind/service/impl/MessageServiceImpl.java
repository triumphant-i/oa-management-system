package com.southwind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.southwind.entity.Message;
import com.southwind.mapper.MessageMapper;
import com.southwind.service.MessageService;
import com.southwind.vo.PageVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

  private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

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
    if (message.getIsTodo() == null) {
      message.setIsTodo(0);
    }
    if (message.getBizType() == null) {
      message.setBizType("approval");
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
      message.setIsTodo(0);
      message.setStatus("正常");
      message.setCreateTime(LocalDateTime.now());
      messages.add(message);
    }
    return saveBatch(messages);
  }

  @Override
  public PageVO listMessagesByCategory(Integer receiverId, String category, int page, int size) {
    QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("receiver_id", receiverId);

    // 按分类筛选
    if (category != null && !category.trim().isEmpty()) {
      switch (category.toLowerCase()) {
        case "todo":
          // 待办：is_todo=1 且 is_read=0
          queryWrapper.eq("is_todo", 1).eq("is_read", 0);
          break;
        case "cc":
          // 抄送：biz_type != 'approval' (未来扩展)
          queryWrapper.ne("biz_type", "approval");
          break;
        case "system":
          // 系统通知：msg_type != 'APPROVAL'
          queryWrapper.ne("msg_type", "APPROVAL");
          break;
        case "all":
        default:
          // 全部：无额外条件
          break;
      }
    }

    // 按create_time倒序
    queryWrapper.orderByDesc("create_time");

    Page<Message> messagePage = new Page<>(page, size);
    Page<Message> result = this.page(messagePage, queryWrapper);

    PageVO pageVO = new PageVO();
    pageVO.setTotal(result.getTotal());
    pageVO.setRecords(result.getRecords());
    pageVO.setCurrent(result.getCurrent());
    pageVO.setSize(result.getSize());
    return pageVO;
  }

  @Override
  public int countTodoMessages(Integer receiverId) {
    QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("receiver_id", receiverId)
            .eq("is_todo", 1)
            .eq("is_read", 0);
    return (int) this.count(queryWrapper);
  }

  @Override
  @Transactional
  public String markMessageAsRead(Integer messageId, Integer receiverId) {
    // 先查询消息，确保有权限访问
    Message message = this.getById(messageId);
    if (message == null || !message.getReceiverId().equals(receiverId)) {
      logger.warn("Unauthorized message read attempt: messageId={}, receiverId={}", messageId, receiverId);
      return null;
    }

    // 标记已读
    UpdateWrapper<Message> updateWrapper = new UpdateWrapper<>();
    updateWrapper.eq("id", messageId).eq("receiver_id", receiverId);
    Message updateMessage = new Message();
    updateMessage.setIsRead(1);
    updateMessage.setReadTime(LocalDateTime.now());
    this.update(updateMessage, updateWrapper);

    logger.info("Message marked as read: messageId={}", messageId);
    return message.getJumpUrl();
  }

  @Override
  @Transactional
  public int markCategoryAsRead(Integer receiverId, String category) {
    QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("receiver_id", receiverId).eq("is_read", 0);

    // 按分类筛选
    if (category != null && !category.trim().isEmpty()) {
      switch (category.toLowerCase()) {
        case "todo":
          queryWrapper.eq("is_todo", 1);
          break;
        case "cc":
          queryWrapper.ne("biz_type", "approval");
          break;
        case "system":
          queryWrapper.ne("msg_type", "APPROVAL");
          break;
        case "all":
        default:
          break;
      }
    }

    UpdateWrapper<Message> updateWrapper = new UpdateWrapper<>();
    updateWrapper.eq("receiver_id", receiverId).eq("is_read", 0);
    if (category != null && !category.trim().isEmpty()) {
      switch (category.toLowerCase()) {
        case "todo":
          updateWrapper.eq("is_todo", 1);
          break;
        case "cc":
          updateWrapper.ne("biz_type", "approval");
          break;
        case "system":
          updateWrapper.ne("msg_type", "APPROVAL");
          break;
      }
    }

    Message updateMessage = new Message();
    updateMessage.setIsRead(1);
    updateMessage.setReadTime(LocalDateTime.now());
    int result = this.baseMapper.update(updateMessage, updateWrapper);
    logger.info("Batch marked as read: receiverId={}, category={}, affected={}", receiverId, category, result);
    return result;
  }

  @Override
  @Transactional
  public int markTodoAsInvalid(Integer bizId, String bizType) {
    UpdateWrapper<Message> updateWrapper = new UpdateWrapper<>();
    updateWrapper.eq("biz_id", bizId);
    if (bizType != null) {
      updateWrapper.eq("biz_type", bizType);
    }
    updateWrapper.eq("is_todo", 1);

    Message updateMessage = new Message();
    updateMessage.setIsTodo(0);
    updateMessage.setUpdateTime(LocalDateTime.now());

    int result = this.baseMapper.update(updateMessage, updateWrapper);
    logger.info("Marked todo messages as invalid: bizId={}, bizType={}, affected={}", bizId, bizType, result);
    return result;
  }
}