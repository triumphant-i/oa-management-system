package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("t_message")
public class Message implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("sender_id")
  private Integer senderId;

  @TableField("sender_name")
  private String senderName;

  @TableField("receiver_id")
  private Integer receiverId;

  @TableField("receiver_name")
  private String receiverName;

  private String title;

  private String content;

  @TableField("msg_type")
  private String msgType;

  @TableField("event_type")
  private String eventType;

  @TableField("biz_type")
  private String bizType;

  @TableField("biz_id")
  private Integer bizId;

  @TableField("related_id")
  private Integer relatedId;

  @TableField("is_read")
  private Integer isRead;

  @TableField("is_todo")
  private Integer isTodo;

  @TableField("jump_url")
  private String jumpUrl;

  @TableField("is_top")
  private Integer isTop;

  private String status;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("read_time")
  private LocalDateTime readTime;

  @TableField("update_time")
  private LocalDateTime updateTime;

  @TableField(exist = false)
  private String senderAvatar;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getSenderId() {
    return senderId;
  }

  public void setSenderId(Integer senderId) {
    this.senderId = senderId;
  }

  public String getSenderName() {
    return senderName;
  }

  public void setSenderName(String senderName) {
    this.senderName = senderName;
  }

  public Integer getReceiverId() {
    return receiverId;
  }

  public void setReceiverId(Integer receiverId) {
    this.receiverId = receiverId;
  }

  public String getReceiverName() {
    return receiverName;
  }

  public void setReceiverName(String receiverName) {
    this.receiverName = receiverName;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getMsgType() {
    return msgType;
  }

  public void setMsgType(String msgType) {
    this.msgType = msgType;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getBizType() {
    return bizType;
  }

  public void setBizType(String bizType) {
    this.bizType = bizType;
  }

  public Integer getBizId() {
    return bizId;
  }

  public void setBizId(Integer bizId) {
    this.bizId = bizId;
  }

  public Integer getRelatedId() {
    return relatedId;
  }

  public void setRelatedId(Integer relatedId) {
    this.relatedId = relatedId;
  }

  public Integer getIsRead() {
    return isRead;
  }

  public void setIsRead(Integer isRead) {
    this.isRead = isRead;
  }

  public Integer getIsTodo() {
    return isTodo;
  }

  public void setIsTodo(Integer isTodo) {
    this.isTodo = isTodo;
  }

  public String getJumpUrl() {
    return jumpUrl;
  }

  public void setJumpUrl(String jumpUrl) {
    this.jumpUrl = jumpUrl;
  }

  public Integer getIsTop() {
    return isTop;
  }

  public void setIsTop(Integer isTop) {
    this.isTop = isTop;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public LocalDateTime getReadTime() {
    return readTime;
  }

  public void setReadTime(LocalDateTime readTime) {
    this.readTime = readTime;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public String getSenderAvatar() {
    return senderAvatar;
  }

  public void setSenderAvatar(String senderAvatar) {
    this.senderAvatar = senderAvatar;
  }
}