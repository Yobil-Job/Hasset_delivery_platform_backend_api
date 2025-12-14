package com.kuru.delivery.location.service;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisLocationService {

    private final StringRedisTemplate redisTemplate;
    private static final String DRIVER_GEO_KEY = "drivers:locations";

    public RedisLocationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateDriverLocation(String driverId, double latitude, double longitude) {
        // Store driver location in a geospatial index
        redisTemplate.opsForGeo().add(DRIVER_GEO_KEY, new Point(longitude, latitude), driverId);
    }

    public Point getDriverLocation(String driverId) {
        // Retrieve driver location
        var positions = redisTemplate.opsForGeo().position(DRIVER_GEO_KEY, driverId);
        if (positions != null && !positions.isEmpty()) {
            return positions.get(0);
        }
        return null;
    }
}
