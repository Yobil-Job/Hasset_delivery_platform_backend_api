package com.kuru.delivery.message.dto;

import java.time.LocalDateTime;

public class MessageResponse {
    private Long id;
    private String orderNumber;
    private Long senderId;
    private String senderName;
    private Long recipientId;
    private String message;
    private String messageType;
    private String fileUrl;
    private LocalDateTime timestamp;
    private boolean isRead;

    public MessageResponse() {}

    public MessageResponse(Long id, String orderNumber, Long senderId, String senderName, 
                          Long recipientId, String message, String messageType, String fileUrl,
                          LocalDateTime timestamp, boolean isRead) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.message = message;
        this.messageType = messageType;
        this.fileUrl = fileUrl;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }
}

