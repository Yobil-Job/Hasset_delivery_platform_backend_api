package com.kuru.delivery.order.repository;

import com.kuru.delivery.order.model.DeliveryProof;
import com.kuru.delivery.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, Long> {
    
    List<DeliveryProof> findByOrderOrderByUploadedAtDesc(Order order);
}

