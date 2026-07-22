package com.southwind.event;

import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ApprovalEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public enum EventType {
        SUBMITTED("SUBMITTED", "你有一条新的待审批单据", true),
        APPROVED_NODE("APPROVED_NODE", "有新的待审批单据", true),
        APPROVED_FINAL("APPROVED_FINAL", "你的XX单据已审批通过", false),
        REJECTED("REJECTED", "你的单据被驳回", false),
        WITHDRAWN("WITHDRAWN", "该单据已被撤回", false),
        CC_ADDED("CC_ADDED", "你被添加为该单据的抄送人", false);

        private final String code;
        private final String defaultMessage;
        private final boolean isTodo;

        EventType(String code, String defaultMessage, boolean isTodo) {
            this.code = code;
            this.defaultMessage = defaultMessage;
            this.isTodo = isTodo;
        }

        public String getCode() {
            return code;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }

        public boolean isTodo() {
            return isTodo;
        }
    }

    public enum BizType {
        APPROVAL("approval", "审批单"),
        WORKFLOW("workflow", "工作流"),
        DOCUMENT("document", "公文");

        private final String code;
        private final String name;

        BizType(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }

    private EventType eventType;
    private BizType bizType;
    private Integer bizId;
    private Integer senderId;
    private String senderName;
    private List<Integer> receiverIds = new ArrayList<>();
    private List<String> receiverNames = new ArrayList<>();
    private String title;
    private String content;
    private String jumpUrl;
    private boolean isTodo;
    private LocalDateTime createTime;

    public ApprovalEvent(Object source) {
        super(source);
        this.createTime = LocalDateTime.now();
    }

    public static ApprovalEventBuilder builder() {
        return new ApprovalEventBuilder();
    }

    public static class ApprovalEventBuilder {
        private final ApprovalEvent event;

        public ApprovalEventBuilder() {
            this.event = new ApprovalEvent(this);
        }

        public ApprovalEventBuilder eventType(EventType eventType) {
            event.eventType = eventType;
            event.isTodo = eventType.isTodo();
            return this;
        }

        public ApprovalEventBuilder bizType(BizType bizType) {
            event.bizType = bizType;
            return this;
        }

        public ApprovalEventBuilder bizId(Integer bizId) {
            event.bizId = bizId;
            return this;
        }

        public ApprovalEventBuilder senderId(Integer senderId) {
            event.senderId = senderId;
            return this;
        }

        public ApprovalEventBuilder senderName(String senderName) {
            event.senderName = senderName;
            return this;
        }

        public ApprovalEventBuilder receiverId(Integer receiverId) {
            if (receiverId != null) {
                event.receiverIds.add(receiverId);
            }
            return this;
        }

        public ApprovalEventBuilder receiverIds(List<Integer> receiverIds) {
            if (receiverIds != null) {
                event.receiverIds.addAll(receiverIds);
            }
            return this;
        }

        public ApprovalEventBuilder receiverName(String receiverName) {
            if (receiverName != null) {
                event.receiverNames.add(receiverName);
            }
            return this;
        }

        public ApprovalEventBuilder receiverNames(List<String> receiverNames) {
            if (receiverNames != null) {
                event.receiverNames.addAll(receiverNames);
            }
            return this;
        }

        public ApprovalEventBuilder title(String title) {
            event.title = title;
            return this;
        }

        public ApprovalEventBuilder content(String content) {
            event.content = content;
            return this;
        }

        public ApprovalEventBuilder jumpUrl(String jumpUrl) {
            event.jumpUrl = jumpUrl;
            return this;
        }

        public ApprovalEventBuilder isTodo(boolean isTodo) {
            event.isTodo = isTodo;
            return this;
        }

        public ApprovalEvent build() {
            return event;
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public BizType getBizType() {
        return bizType;
    }

    public void setBizType(BizType bizType) {
        this.bizType = bizType;
    }

    public Integer getBizId() {
        return bizId;
    }

    public void setBizId(Integer bizId) {
        this.bizId = bizId;
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

    public List<Integer> getReceiverIds() {
        return receiverIds;
    }

    public void setReceiverIds(List<Integer> receiverIds) {
        this.receiverIds = receiverIds;
    }

    public List<String> getReceiverNames() {
        return receiverNames;
    }

    public void setReceiverNames(List<String> receiverNames) {
        this.receiverNames = receiverNames;
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

    public String getJumpUrl() {
        return jumpUrl;
    }

    public void setJumpUrl(String jumpUrl) {
        this.jumpUrl = jumpUrl;
    }

    public boolean isTodo() {
        return isTodo;
    }

    public void setTodo(boolean todo) {
        isTodo = todo;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "ApprovalEvent{" +
                "eventType=" + eventType +
                ", bizType=" + bizType +
                ", bizId=" + bizId +
                ", senderId=" + senderId +
                ", senderName='" + senderName + '\'' +
                ", receiverIds=" + receiverIds +
                ", isTodo=" + isTodo +
                ", createTime=" + createTime +
                '}';
    }
}