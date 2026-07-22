package com.southwind.controller;

import com.southwind.common.UserContext;
import com.southwind.entity.Message;
import com.southwind.service.MessageService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import com.southwind.vo.ResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {

  private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

  @Autowired
  private MessageService messageService;

  /**
   * 分页查询当前登录人的消息
   * 支持按分类筛选：all=全部, todo=待办(is_todo=1且is_read=0), cc=抄送, system=系统通知
   * 按create_time倒序
   * 
   * @param category 分类：all/todo/cc/system
   * @param page 页码（从1开始）
   * @param size 每页大小
   */
  @GetMapping("/list")
  public ResultVO list(
      @RequestParam(value = "category", required = false, defaultValue = "all") String category,
      @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
      @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
    
    UserContext.UserInfo currentUser = UserContext.getCurrentUser();
    if (currentUser == null) {
      return ResultVOUtil.fail("未登录");
    }

    try {
      PageVO pageVO = messageService.listMessagesByCategory(currentUser.getUserId(), category, page, size);
      return ResultVOUtil.success(pageVO);
    } catch (Exception e) {
      logger.error("Error listing messages: {}", e.getMessage(), e);
      return ResultVOUtil.fail("查询消息失败");
    }
  }

  /**
   * 获取当前登录人的未读待办数量
   * 用于首页/底部导航角标
   */
  @GetMapping("/todoCount")
  public ResultVO todoCount() {
    UserContext.UserInfo currentUser = UserContext.getCurrentUser();
    if (currentUser == null) {
      return ResultVOUtil.fail("未登录");
    }

    try {
      int count = messageService.countTodoMessages(currentUser.getUserId());
      Map<String, Integer> result = new HashMap<>();
      result.put("count", count);
      return ResultVOUtil.success(result);
    } catch (Exception e) {
      logger.error("Error counting todo messages: {}", e.getMessage(), e);
      return ResultVOUtil.fail("获取待办数量失败");
    }
  }

  /**
   * 标记单条消息已读
   * 返回消息的jump_url供前端跳转
   * 只能标记自己的消息
   */
  @PutMapping("/read/{messageId}")
  public ResultVO markRead(@PathVariable("messageId") Integer messageId) {
    UserContext.UserInfo currentUser = UserContext.getCurrentUser();
    if (currentUser == null) {
      return ResultVOUtil.fail("未登录");
    }

    try {
      String jumpUrl = messageService.markMessageAsRead(messageId, currentUser.getUserId());
      if (jumpUrl == null) {
        return ResultVOUtil.fail("消息不存在或无权访问");
      }
      
      Map<String, String> result = new HashMap<>();
      result.put("jumpUrl", jumpUrl);
      return ResultVOUtil.success(result);
    } catch (Exception e) {
      logger.error("Error marking message as read: {}", e.getMessage(), e);
      return ResultVOUtil.fail("标记已读失败");
    }
  }

  /**
   * 批量标记当前分类下的所有未读消息为已读
   * 
   * @param category 分类：all/todo/cc/system
   */
  @PutMapping("/readAll")
  public ResultVO markAllRead(
      @RequestParam(value = "category", required = false, defaultValue = "all") String category) {
    
    UserContext.UserInfo currentUser = UserContext.getCurrentUser();
    if (currentUser == null) {
      return ResultVOUtil.fail("未登录");
    }

    try {
      int affected = messageService.markCategoryAsRead(currentUser.getUserId(), category);
      Map<String, Integer> result = new HashMap<>();
      result.put("affected", affected);
      return ResultVOUtil.success(result);
    } catch (Exception e) {
      logger.error("Error marking all messages as read: {}", e.getMessage(), e);
      return ResultVOUtil.fail("批量标记已读失败");
    }
  }

  /**
   * 旧接口：查询当前用户所有消息（保留向下兼容）
   */
  @GetMapping("/listOld")
  public ResultVO listOld() {
    UserContext.UserInfo currentUser = UserContext.getCurrentUser();
    if (currentUser == null) {
      return ResultVOUtil.fail("未登录");
    }
    List<Message> messages = messageService.findByReceiverId(currentUser.getUserId());
    return ResultVOUtil.success(messages);
  }

  /**
   * 旧接口：获取未读消息数（保留向下兼容）
   */
  @GetMapping("/countUnread")
  public ResultVO countUnread() {
    UserContext.UserInfo currentUser = UserContext.getCurrentUser();
    if (currentUser == null) {
      return ResultVOUtil.fail("未登录");
    }
    Integer count = messageService.countUnread(currentUser.getUserId());
    Map<String, Integer> result = new HashMap<>();
    result.put("count", count != null ? count : 0);
    return ResultVOUtil.success(result);
  }

  /**
   * 旧接口：标记单条已读（保留向下兼容）
   */
  @PostMapping("/markRead/{id}")
  public ResultVO markReadOld(@PathVariable("id") Integer id) {
    try {
      messageService.markAsRead(id);
      return ResultVOUtil.success(null);
    } catch (Exception e) {
      return ResultVOUtil.fail("操作失败");
    }
  }

  /**
   * 旧接口：标记全部已读（保留向下兼容）
   */
  @PostMapping("/markAllRead")
  public ResultVO markAllReadOld() {
    UserContext.UserInfo currentUser = UserContext.getCurrentUser();
    if (currentUser == null) {
      return ResultVOUtil.fail("未登录");
    }
    try {
      messageService.markAllAsRead(currentUser.getUserId());
      return ResultVOUtil.success(null);
    } catch (Exception e) {
      return ResultVOUtil.fail("操作失败");
    }
  }

  /**
   * 旧接口：发送消息（保留向下兼容）
   */
  @PostMapping("/send")
  public ResultVO send(@RequestBody Message message) {
    UserContext.UserInfo currentUser = UserContext.getCurrentUser();
    if (currentUser == null) {
      return ResultVOUtil.fail("未登录");
    }
    message.setSenderId(currentUser.getUserId());
    message.setSenderName(currentUser.getName());
    boolean success = messageService.sendMessage(message);
    if (success) {
      return ResultVOUtil.success(null);
    } else {
      return ResultVOUtil.fail("发送失败");
    }
  }

  /**
   * 旧接口：消息详情（保留向下兼容）
   */
  @GetMapping("/detail/{id}")
  public ResultVO detail(@PathVariable("id") Integer id) {
    Message message = messageService.getById(id);
    if (message == null) {
      return ResultVOUtil.fail("消息不存在");
    }
    UserContext.UserInfo currentUser = UserContext.getCurrentUser();
    if (currentUser != null && !message.getReceiverId().equals(currentUser.getUserId())) {
      return ResultVOUtil.fail("无权查看该消息");
    }
    if (message.getIsRead() == null || message.getIsRead() == 0) {
      messageService.markAsRead(id);
    }
    return ResultVOUtil.success(message);
  }

  /**
   * 旧接口：删除消息（保留向下兼容）
   */
  @DeleteMapping("/delete/{id}")
  public ResultVO delete(@PathVariable("id") Integer id) {
    UserContext.UserInfo currentUser = UserContext.getCurrentUser();
    if (currentUser == null) {
      return ResultVOUtil.fail("未登录");
    }
    Message message = messageService.getById(id);
    if (message == null) {
      return ResultVOUtil.fail("消息不存在");
    }
    if (!message.getReceiverId().equals(currentUser.getUserId())) {
      return ResultVOUtil.fail("无权删除该消息");
    }
    boolean success = messageService.removeById(id);
    if (success) {
      return ResultVOUtil.success(null);
    } else {
      return ResultVOUtil.fail("删除失败");
    }
  }
}