package com.kuru.delivery.message.controller;

import com.kuru.delivery.message.dto.MessageResponse;
import com.kuru.delivery.message.dto.SendMessageRequest;
import com.kuru.delivery.message.service.MessageService;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/order/{orderNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER')")
    public ResponseEntity<?> getMessages(@PathVariable String orderNumber) {
        try {
            Long userId = getCurrentUserId();
            List<MessageResponse> messages = messageService.getMessagesByOrder(orderNumber, userId);
            return ResponseEntity.ok(messages);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER')")
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageRequest request) {
        try {
            Long senderId = getCurrentUserId();
            MessageResponse message = messageService.sendMessage(senderId, request);
            
            // Broadcast via WebSocket to all participants
            messagingTemplate.convertAndSend("/topic/chat/" + request.getOrderNumber(), message);
            
            // Send notification to recipient
            String senderName = message.getSenderName();
            String preview = message.getMessage();
            if (preview.length() > 50) {
                preview = preview.substring(0, 50) + "...";
            }
            
            // Create notification payload
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_MESSAGE");
            notification.put("orderNumber", request.getOrderNumber());
            notification.put("messageId", message.getId());
            notification.put("senderName", senderName);
            notification.put("preview", preview);
            
            // Send notification to recipient's user ID
            messagingTemplate.convertAndSend("/topic/notifications/" + request.getRecipientId(), notification);
            
            return ResponseEntity.ok(message);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER')")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Long userId = getCurrentUserId();
        long count = messageService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER')")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            messageService.markAsRead(id, userId);
            return ResponseEntity.ok(Map.of("message", "Message marked as read"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/order/{orderNumber}/read-all")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER')")
    public ResponseEntity<?> markOrderMessagesAsRead(@PathVariable String orderNumber) {
        try {
            Long userId = getCurrentUserId();
            messageService.markOrderMessagesAsRead(orderNumber, userId);
            return ResponseEntity.ok(Map.of("message", "All messages marked as read"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private Long getCurrentUserId() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}

