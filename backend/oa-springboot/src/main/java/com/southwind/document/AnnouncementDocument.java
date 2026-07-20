package com.southwind.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告ES文档实体
 * 用于全文检索
 */
@Data
@Document(indexName = "announcement_index")
public class AnnouncementDocument implements Serializable {

    @Id
    private Integer id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;

    @Field(type = FieldType.Integer)
    private Integer publisherId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String publisherName;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String priority;

    @Field(type = FieldType.Integer)
    private Integer isTop;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date)
    private LocalDateTime publishTime;

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
}