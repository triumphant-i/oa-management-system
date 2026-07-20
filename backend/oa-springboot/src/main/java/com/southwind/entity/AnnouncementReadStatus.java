package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_announcement_read_status")
public class AnnouncementReadStatus implements Serializable {

  private static final long serialVersionUID=1L;

  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("announcement_id")
  private Integer announcementId;

  @TableField("employee_id")
  private Integer employeeId;

  @TableField("is_read")
  private Integer isRead;

  @TableField("read_time")
  private LocalDateTime readTime;

  @TableField("create_time")
  private LocalDateTime createTime;
}