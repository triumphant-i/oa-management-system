package com.southwind.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID=1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    private String user;

    private String action;

    @TableField("`desc`")
    private String desc;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("create_time")
    private LocalDateTime createTime;

    // 手动添加 getter 方法
    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUser() {
        return user;
    }

    public String getAction() {
        return action;
    }

    public String getDesc() {
        return desc;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    // 手动添加 setter 方法
    public void setId(Integer id) {
        this.id = id;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}