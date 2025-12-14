package com.kuru.delivery.payment.repository;

import com.kuru.delivery.payment.model.PaymentTransaction;
import com.kuru.delivery.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTxRef(String txRef);
    Optional<PaymentTransaction> findByChapaReference(String chapaReference);
    List<PaymentTransaction> findByOrderId(Long orderId);
    List<PaymentTransaction> findByStatus(PaymentStatus status);
    List<PaymentTransaction> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}

