package com.southwind.controller;

import com.southwind.common.UserContext;
import com.southwind.entity.Message;
import com.southwind.service.MessageService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {

  @Autowired
  private MessageService messageService;

  @GetMapping("/list")
  public ResultVO list() {
    UserContext.UserInfo currentUser = UserContext.getCurrentUser();
    if (currentUser == null) {
      return ResultVOUtil.fail("未登录");
    }
    List<Message> messages = messageService.findByReceiverId(currentUser.getUserId());
    return ResultVOUtil.success(messages);
  }

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

  @PostMapping("/markRead/{id}")
  public ResultVO markRead(@PathVariable("id") Integer id) {
    try {
      messageService.markAsRead(id);
      return ResultVOUtil.success(null);
    } catch (Exception e) {
      return ResultVOUtil.fail("操作失败");
    }
  }

  @PostMapping("/markAllRead")
  public ResultVO markAllRead() {
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