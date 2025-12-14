package com.kuru.delivery.location.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.kuru.delivery.location.repository.LocationRepository;

@Service
public class LocationCleanupService {

    private static final Logger log = LoggerFactory.getLogger(LocationCleanupService.class);

    private final LocationRepository locationRepository;

    public LocationCleanupService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    // Run once per day at 03:00 server time
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldLocations() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(2);
        long beforeCount = locationRepository.count();
        locationRepository.deleteByTimeStampBefore(cutoff);
        long afterCount = locationRepository.count();
        long deleted = beforeCount - afterCount;
        if (deleted > 0) {
            log.info("Location cleanup: deleted {} location rows older than {}", deleted, cutoff);
        }
    }
}
