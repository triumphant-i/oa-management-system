package com.southwind.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.southwind.entity.Message;
import com.southwind.vo.PageVO;

import java.util.List;

public interface MessageService extends IService<Message> {

  Integer countUnread(Integer receiverId);

  List<Message> findByReceiverId(Integer receiverId);

  void markAsRead(Integer id);

  void markAllAsRead(Integer receiverId);

  boolean sendMessage(Message message);

  boolean sendBatchMessage(List<Integer> receiverIds, String title, String content, String msgType);

  /**
   * 分页查询当前登录人的消息
   * 支持按分类筛选：all=全部, todo=待办(is_todo=1且is_read=0), cc=抄送, system=系统通知
   */
  PageVO listMessagesByCategory(Integer receiverId, String category, int page, int size);

  /**
   * 获取当前登录人的未读待办数量（is_todo=1 且 is_read=0）
   */
  int countTodoMessages(Integer receiverId);

  /**
   * 标记单条消息已读，返回jump_url供前端跳转
   */
  String markMessageAsRead(Integer messageId, Integer receiverId);

  /**
   * 批量标记当前分类下的所有未读消息为已读
   */
  int markCategoryAsRead(Integer receiverId, String category);

  /**
   * 将指定业务ID的待办消息标记为失效（is_todo=0）
   * 用于审批撤回等场景
   */
  int markTodoAsInvalid(Integer bizId, String bizType);
}