package com.kuru.delivery.location.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kuru.delivery.location.model.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

	void deleteByTimeStampBefore(LocalDateTime cutoff);
}
