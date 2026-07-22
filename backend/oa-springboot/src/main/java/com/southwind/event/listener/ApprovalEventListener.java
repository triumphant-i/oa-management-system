package com.southwind.event.listener;

import com.southwind.entity.Message;
import com.southwind.event.ApprovalEvent;
import com.southwind.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审批事件监听器
 * 监听ApprovalEvent，根据事件类型生成对应的消息通知
 * 体现"消息模块只订阅事件，不感知审批业务规则"的解耦设计
 */
@Component
public class ApprovalEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalEventListener.class);

    @Autowired
    private MessageService messageService;

    /**
     * 监听审批事件，自动生成消息通知
     */
    @EventListener
    @Transactional
    public void onApprovalEvent(ApprovalEvent event) {
        if (event == null || event.getEventType() == null) {
            logger.warn("Received invalid ApprovalEvent");
            return;
        }

        logger.info("Processing ApprovalEvent: eventType={}, bizId={}, receiverIds={}",
                event.getEventType(), event.getBizId(), event.getReceiverIds());

        try {
            // 为每个接收人生成一条消息
            for (int i = 0; i < event.getReceiverIds().size(); i++) {
                Integer receiverId = event.getReceiverIds().get(i);
                String receiverName = i < event.getReceiverNames().size() 
                    ? event.getReceiverNames().get(i) 
                    : "";

                Message message = new Message();
                message.setSenderId(event.getSenderId());
                message.setSenderName(event.getSenderName());
                message.setReceiverId(receiverId);
                message.setReceiverName(receiverName);
                
                // 设置消息内容
                message.setTitle(event.getTitle());
                message.setContent(event.getContent());
                message.setEventType(event.getEventType().getCode());
                message.setBizType(event.getBizType() != null ? event.getBizType().getCode() : "approval");
                message.setBizId(event.getBizId());
                message.setJumpUrl(event.getJumpUrl());
                
                // 设置待办标记和消息类型
                message.setIsTodo(event.isTodo() ? 1 : 0);
                message.setMsgType("APPROVAL");
                
                // 初始化其他字段
                message.setIsRead(0);
                message.setIsTop(0);
                message.setStatus("正常");
                message.setCreateTime(LocalDateTime.now());
                message.setRelatedId(event.getBizId());

                boolean result = messageService.sendMessage(message);
                if (result) {
                    logger.info("Message sent successfully: receiverId={}, eventType={}", 
                        receiverId, event.getEventType());
                } else {
                    logger.error("Failed to send message: receiverId={}, eventType={}", 
                        receiverId, event.getEventType());
                }
            }
        } catch (Exception e) {
            logger.error("Error processing ApprovalEvent: {}", e.getMessage(), e);
            // 事件监听器异常不影响业务主流程
        }
    }

    /**
     * 处理WITHDRAWN事件的特殊逻辑：将该审批人之前收到的待办消息标记为失效
     */
    @EventListener
    @Transactional
    public void onApprovalWithdrawn(ApprovalEvent event) {
        if (event.getEventType() != ApprovalEvent.EventType.WITHDRAWN) {
            return;
        }

        try {
            logger.info("Processing WITHDRAWN event: bizId={}, receiverIds={}", 
                event.getBizId(), event.getReceiverIds());

            String bizType = event.getBizType() != null ? event.getBizType().getCode() : "approval";
            int affected = messageService.markTodoAsInvalid(event.getBizId(), bizType);
            logger.info("Marked {} todo messages as invalid for bizId={}", affected, event.getBizId());
            
        } catch (Exception e) {
            logger.error("Error processing WITHDRAWN event: {}", e.getMessage(), e);
        }
    }
}