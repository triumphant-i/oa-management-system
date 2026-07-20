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
@TableName("t_attachment")
public class Attachment implements Serializable {

  private static final long serialVersionUID=1L;

  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("business_id")
  private Integer businessId;

  @TableField("business_type")
  private String businessType;

  @TableField("file_name")
  private String fileName;

  @TableField("file_path")
  private String filePath;

  @TableField("file_size")
  private Long fileSize;

  @TableField("file_type")
  private String fileType;

  @TableField("uploader_id")
  private Integer uploaderId;

  @TableField("uploader_name")
  private String uploaderName;

  private LocalDateTime createTime;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getBusinessId() { return businessId; }
  public void setBusinessId(Integer businessId) { this.businessId = businessId; }
  public String getBusinessType() { return businessType; }
  public void setBusinessType(String businessType) { this.businessType = businessType; }
  public String getFileName() { return fileName; }
  public void setFileName(String fileName) { this.fileName = fileName; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public Long getFileSize() { return fileSize; }
  public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
  public String getFileType() { return fileType; }
  public void setFileType(String fileType) { this.fileType = fileType; }
  public Integer getUploaderId() { return uploaderId; }
  public void setUploaderId(Integer uploaderId) { this.uploaderId = uploaderId; }
  public String getUploaderName() { return uploaderName; }
  public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
