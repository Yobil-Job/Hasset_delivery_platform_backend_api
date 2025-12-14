package com.kuru.delivery.user.service;

import com.kuru.delivery.user.dto.RecentAddressResponse;
import com.kuru.delivery.user.model.RecentAddress;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.RecentAddressRepository;
import com.kuru.delivery.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RecentAddressService {

    private final RecentAddressRepository recentAddressRepository;
    private final UserRepository userRepository;
    private static final int MAX_RECENT_ADDRESSES = 10;

    public RecentAddressService(RecentAddressRepository recentAddressRepository, UserRepository userRepository) {
        this.recentAddressRepository = recentAddressRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void saveOrUpdateAddress(Long userId, String address, double latitude, double longitude, String type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Check if address already exists for this user
        Optional<RecentAddress> existing = recentAddressRepository.findByUserAndAddressAndLatitudeAndLongitude(
                user, address, latitude, longitude);

        if (existing.isPresent()) {
            // Update lastUsed timestamp
            RecentAddress recentAddress = existing.get();
            recentAddress.setLastUsed(LocalDateTime.now());
            if (type != null) {
                recentAddress.setType(type);
            }
            recentAddressRepository.save(recentAddress);
        } else {
            // Create new recent address
            RecentAddress recentAddress = new RecentAddress(user, address, latitude, longitude, type);
            recentAddressRepository.save(recentAddress);

            // Check if we need to remove oldest addresses
            long count = recentAddressRepository.countByUser(user);
            if (count > MAX_RECENT_ADDRESSES) {
                List<RecentAddress> allAddresses = recentAddressRepository.findRecentAddressesByUser(user);
                // Keep only the most recent MAX_RECENT_ADDRESSES
                List<RecentAddress> toDelete = allAddresses.subList(MAX_RECENT_ADDRESSES, allAddresses.size());
                recentAddressRepository.deleteAll(toDelete);
            }
        }
    }

    @Transactional
    public void updateLastUsed(Long addressId, Long userId) {
        RecentAddress recentAddress = recentAddressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Recent address not found"));

        // Verify ownership
        if (!recentAddress.getUser().getId().equals(userId)) {
            throw new IllegalStateException("You can only update your own addresses");
        }

        recentAddress.setLastUsed(LocalDateTime.now());
        recentAddressRepository.save(recentAddress);
    }

    public List<RecentAddressResponse> getRecentAddresses(Long userId, Integer limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<RecentAddress> addresses = recentAddressRepository.findRecentAddressesByUser(user);
        
        int maxResults = (limit != null && limit > 0) ? limit : 5;
        if (addresses.size() > maxResults) {
            addresses = addresses.subList(0, maxResults);
        }

        return addresses.stream()
                .map(ra -> new RecentAddressResponse(
                        ra.getId(),
                        ra.getAddress(),
                        ra.getLatitude(),
                        ra.getLongitude(),
                        ra.getLastUsed(),
                        ra.getType()
                ))
                .collect(Collectors.toList());
    }
}

