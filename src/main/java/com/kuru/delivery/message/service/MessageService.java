package com.kuru.delivery.message.service;

import com.kuru.delivery.message.dto.MessageResponse;
import com.kuru.delivery.message.dto.SendMessageRequest;
import com.kuru.delivery.message.model.Message;
import com.kuru.delivery.message.repository.MessageRepository;
import com.kuru.delivery.order.model.Order;
import com.kuru.delivery.order.repository.OrderRepository;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        Order order = orderRepository.findByOrderNumber(request.getOrderNumber())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Sender not found"));

        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new EntityNotFoundException("Recipient not found"));

        // Verify sender is either customer or driver of this order
        Long driverUserId = order.getDriver() != null && order.getDriver().getUser() != null 
            ? order.getDriver().getUser().getId() : null;
        
        if (!order.getCustomerId().equals(senderId) && 
            (driverUserId == null || !driverUserId.equals(senderId))) {
            throw new IllegalStateException("You can only send messages for your own orders");
        }

        // Verify recipient is the other party (customer or driver)
        if (!order.getCustomerId().equals(request.getRecipientId()) && 
            (driverUserId == null || !driverUserId.equals(request.getRecipientId()))) {
            throw new IllegalStateException("Invalid recipient for this order");
        }

        Message message = new Message();
        message.setOrder(order);
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setMessage(request.getMessage());
        message.setMessageType(request.getMessageType() != null ? request.getMessageType() : "text");
        message.setFileUrl(request.getFileUrl());
        message.setRead(false);

        Message saved = messageRepository.save(message);
        return mapToResponse(saved);
    }

    public List<MessageResponse> getMessagesByOrder(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        // Verify user is part of this order
        Long driverUserId = order.getDriver() != null && order.getDriver().getUser() != null 
            ? order.getDriver().getUser().getId() : null;
        
        if (!order.getCustomerId().equals(userId) && 
            (driverUserId == null || !driverUserId.equals(userId))) {
            throw new IllegalStateException("You can only view messages for your own orders");
        }

        List<Message> messages = messageRepository.findMessagesByOrder(order);
        return messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return messageRepository.countUnreadMessagesByRecipient(user);
    }

    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        // Verify user is the recipient
        if (!message.getRecipient().getId().equals(userId)) {
            throw new IllegalStateException("You can only mark your own messages as read");
        }

        message.setRead(true);
        messageRepository.save(message);
    }

    @Transactional
    public void markOrderMessagesAsRead(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        List<Message> messages = messageRepository.findMessagesByOrder(order);
        messages.stream()
                .filter(m -> m.getRecipient().getId().equals(userId) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });
    }

    private MessageResponse mapToResponse(Message message) {
        String senderName = message.getSender().getFirstname() + " " + message.getSender().getLastname();
        return new MessageResponse(
                message.getId(),
                message.getOrder().getOrderNumber(),
                message.getSender().getId(),
                senderName,
                message.getRecipient().getId(),
                message.getMessage(),
                message.getMessageType(),
                message.getFileUrl(),
                message.getCreatedAt(),
                message.isRead()
        );
    }
}

