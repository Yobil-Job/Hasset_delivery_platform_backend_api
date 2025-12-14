package com.kuru.delivery.location.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuru.delivery.location.model.Location;
import com.kuru.delivery.location.repository.LocationRepository;
import com.kuru.delivery.location.service.RedisLocationService;

@RestController
@RequestMapping("/api/location")
public class LocationController {
	
	private final LocationRepository locationRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final RedisLocationService redisLocationService;
	
	// Track last DB save time per driver (for throttling persistent history)
	private final Map<String, LocalDateTime> lastDbSave = new ConcurrentHashMap<>();
	// Only persist to SQL at most once per minute per driver. Real-time goes via Redis + WebSocket.
	private static final int DB_SAVE_INTERVAL_SECONDS = 60;

	public LocationController(LocationRepository locationRepository, SimpMessagingTemplate messagingTemplate, RedisLocationService redisLocationService) {
		this.locationRepository = locationRepository;
		this.messagingTemplate = messagingTemplate;
		this.redisLocationService = redisLocationService;
	}
	
	@PostMapping
	@PreAuthorize("hasRole('DRIVER')")
	public Location updateLocation(@RequestBody Location location) {
		location.setTimeStamp(LocalDateTime.now()); 
		String deviceId = location.getDeviceId();
		
		// 1. ALWAYS Update Redis (Real-time state - no throttling)
		redisLocationService.updateDriverLocation(deviceId, location.getLatitude(), location.getLongitude());
		
		// 2. ALWAYS Broadcast to WebSocket (Live tracking - no throttling)
		messagingTemplate.convertAndSend("/topic/locations", location);
		
		// 3. CONDITIONALLY Save to DB (throttled to every DB_SAVE_INTERVAL_SECONDS)
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime lastSave = lastDbSave.get(deviceId);
		
		if (lastSave == null || Duration.between(lastSave, now).getSeconds() >= DB_SAVE_INTERVAL_SECONDS) {
			lastDbSave.put(deviceId, now);
			return locationRepository.save(location);
		}
		
		// Don't save to DB, but return the location object (success response)
		return location;
	}

}
