package com.kuru.delivery.message.repository;

import com.kuru.delivery.message.model.Message;
import com.kuru.delivery.order.model.Order;
import com.kuru.delivery.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByOrderOrderByCreatedAtAsc(Order order);
    
    @Query("SELECT m FROM Message m WHERE m.order = :order ORDER BY m.createdAt ASC")
    List<Message> findMessagesByOrder(@Param("order") Order order);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient = :recipient AND m.isRead = false")
    long countUnreadMessagesByRecipient(@Param("recipient") User recipient);
    
    @Query("SELECT m FROM Message m WHERE m.recipient = :recipient AND m.isRead = false ORDER BY m.createdAt DESC")
    List<Message> findUnreadMessagesByRecipient(@Param("recipient") User recipient);
}

