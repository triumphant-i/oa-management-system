package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会议室实体类
 * 新建实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_meeting_room")
public class MeetingRoom implements Serializable {

  private static final long serialVersionUID=1L;

  /**
   * 会议室ID
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  /**
   * 会议室名称
   */
  private String name;

  /**
   * 容纳人数
   */
  private Integer capacity;

  /**
   * 设备(投影仪/白板/音响等)
   */
  private String equipment;

  /**
   * 位置
   */
  private String location;

  /**
   * 状态(可用/维修中/已停用)
   */
  private String status;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 更新时间
   */
  private LocalDateTime updateTime;

  // 手动添加 getter 方法（解决 Lombok 编译问题）
  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public String getEquipment() {
    return equipment;
  }

  public String getLocation() {
    return location;
  }

  public String getStatus() {
    return status;
  }

  // 手动添加 setter 方法（解决 Lombok 编译问题）
  public void setId(Integer id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public void setEquipment(String equipment) {
    this.equipment = equipment;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}