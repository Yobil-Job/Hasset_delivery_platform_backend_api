package com.kuru.delivery.user.repository;

import com.kuru.delivery.user.model.RecentAddress;
import com.kuru.delivery.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecentAddressRepository extends JpaRepository<RecentAddress, Long> {
    
    List<RecentAddress> findByUserOrderByLastUsedDesc(User user);
    
    @Query("SELECT ra FROM RecentAddress ra WHERE ra.user = :user ORDER BY ra.lastUsed DESC")
    List<RecentAddress> findRecentAddressesByUser(@Param("user") User user);
    
    Optional<RecentAddress> findByUserAndAddressAndLatitudeAndLongitude(
        User user, String address, double latitude, double longitude
    );
    
    @Query("SELECT COUNT(ra) FROM RecentAddress ra WHERE ra.user = :user")
    long countByUser(@Param("user") User user);
    
    @Query("SELECT ra FROM RecentAddress ra WHERE ra.user = :user ORDER BY ra.lastUsed DESC")
    List<RecentAddress> findOldestByUser(@Param("user") User user);
}

