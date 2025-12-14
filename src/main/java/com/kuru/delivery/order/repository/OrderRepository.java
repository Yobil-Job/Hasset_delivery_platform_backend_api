package com.kuru.delivery.order.repository;

import com.kuru.delivery.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByCustomerId(Long customerId);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    List<Order> findByDriverAndStatusIn(com.kuru.delivery.driver.model.Driver driver, List<com.kuru.delivery.order.model.OrderStatus> statuses);
    
    List<Order> findByDriver(com.kuru.delivery.driver.model.Driver driver);
}
