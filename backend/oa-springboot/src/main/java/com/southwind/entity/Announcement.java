package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告实体类
 * 新建实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_announcement")
public class Announcement implements Serializable {

  private static final long serialVersionUID=1L;

  /**
   * 公告ID
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  /**
   * 公告标题
   */
  private String title;

  /**
   * 公告内容
   */
  private String content;

  /**
   * 发布人ID
   */
  @TableField("publisher_id")
  private Integer publisherId;

  /**
   * 发布人姓名
   */
  @TableField("publisher_name")
  private String publisherName;

  /**
   * 分类(通知/制度/会议纪要)
   */
  private String category;

  /**
   * 优先级(紧急/重要/普通)
   */
  private String priority;

  /**
   * 是否置顶
   */
  @TableField("is_top")
  private Integer isTop;

  /**
   * 状态(草稿/已发布/已撤回)
   */
  private String status;

  /**
   * 发布时间
   */
  @TableField("publish_time")
  private LocalDateTime publishTime;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 更新时间
   */
  private LocalDateTime updateTime;

  /**
   * 是否已读（非数据库字段，用于前端展示）
   */
  @TableField(exist = false)
  private Integer isRead;

  // 手动添加 getter 方法（解决 Lombok 编译问题）
  public Integer getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public Integer getPublisherId() {
    return publisherId;
  }

  public String getPublisherName() {
    return publisherName;
  }

  public String getCategory() {
    return category;
  }

  public String getPriority() {
    return priority;
  }

  public Integer getIsTop() {
    return isTop;
  }

  public String getStatus() {
    return status;
  }

  public LocalDateTime getPublishTime() {
    return publishTime;
  }

  // 手动添加 setter 方法
  public void setId(Integer id) {
    this.id = id;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setPublisherId(Integer publisherId) {
    this.publisherId = publisherId;
  }

  public void setPublisherName(String publisherName) {
    this.publisherName = publisherName;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public void setIsTop(Integer isTop) {
    this.isTop = isTop;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setPublishTime(LocalDateTime publishTime) {
    this.publishTime = publishTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public Integer getIsRead() {
    return isRead;
  }

  public void setIsRead(Integer isRead) {
    this.isRead = isRead;
  }

}