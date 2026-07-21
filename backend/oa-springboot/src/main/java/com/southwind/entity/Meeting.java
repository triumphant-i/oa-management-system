package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会议实体类
 * 新建实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_meeting")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Meeting implements Serializable {

  private static final long serialVersionUID=1L;

  /**
   * 会议ID
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  /**
   * 会议主题
   */
  private String title;

  /**
   * 会议室ID
   */
  @TableField("room_id")
  private Integer roomId;

  /**
   * 会议室名称
   */
  @TableField("room_name")
  private String roomName;

  /**
   * 组织者ID
   */
  @TableField("organizer_id")
  private Integer organizerId;

  /**
   * 组织者姓名
   */
  @TableField("organizer_name")
  private String organizerName;

  /**
   * 参与人姓名（逗号分隔）
   */
  private String participants;

  /**
   * 参与人ID列表（逗号分隔）
   */
  @TableField("participant_ids")
  private String participantIds;

  /**
   * 开始时间
   */
  @TableField("start_time")
  private LocalDateTime startTime;

  /**
   * 结束时间
   */
  @TableField("end_time")
  private LocalDateTime endTime;

  /**
   * 状态(已预约/已取消/已完成)
   */
  private String status;

  /**
   * 备注
   */
  private String remark;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 更新时间
   */
  private LocalDateTime updateTime;

  // 手动添加必要的 getter 方法（解决 Lombok 编译问题）
  public Integer getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public Integer getRoomId() {
    return roomId;
  }

  public String getRoomName() {
    return roomName;
  }

  public Integer getOrganizerId() {
    return organizerId;
  }

  public String getOrganizerName() {
    return organizerName;
  }

  public String getParticipantIds() {
    return participantIds;
  }

  public String getParticipants() {
    return participants;
  }

  public String getRemark() {
    return remark;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public String getStatus() {
    return status;
  }

  // 手动添加必要的 setter 方法
  public void setId(Integer id) {
    this.id = id;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setRoomId(Integer roomId) {
    this.roomId = roomId;
  }

  public void setRoomName(String roomName) {
    this.roomName = roomName;
  }

  public void setOrganizerId(Integer organizerId) {
    this.organizerId = organizerId;
  }

  public void setOrganizerName(String organizerName) {
    this.organizerName = organizerName;
  }

  public void setParticipantIds(String participantIds) {
    this.participantIds = participantIds;
  }

  public void setParticipants(String participants) {
    this.participants = participants;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

}