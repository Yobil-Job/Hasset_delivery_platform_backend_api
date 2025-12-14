package com.kuru.delivery.driver.repository;

import com.kuru.delivery.driver.model.Driver;
import com.kuru.delivery.driver.model.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUserId(Long userId);
    Optional<Driver> findByUser(com.kuru.delivery.user.model.User user);
    List<Driver> findByStatus(DriverStatus status);
}
