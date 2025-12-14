package com.kuru.delivery.user.repository;

import com.kuru.delivery.user.model.SavedAddress;
import com.kuru.delivery.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedAddressRepository extends JpaRepository<SavedAddress, Long> {
    
    List<SavedAddress> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);
    
    Optional<SavedAddress> findByUserAndId(User user, Long id);
    
    Optional<SavedAddress> findByUserAndIsDefaultTrue(User user);
    
    @Query("SELECT COUNT(sa) FROM SavedAddress sa WHERE sa.user = :user AND sa.isDefault = true")
    long countDefaultAddressesByUser(@Param("user") User user);
}

