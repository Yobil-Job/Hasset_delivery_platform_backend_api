package com.kuru.delivery.user.service;

import com.kuru.delivery.user.dto.SavedAddressRequest;
import com.kuru.delivery.user.dto.SavedAddressResponse;
import com.kuru.delivery.user.model.SavedAddress;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.SavedAddressRepository;
import com.kuru.delivery.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SavedAddressService {

    private final SavedAddressRepository savedAddressRepository;
    private final UserRepository userRepository;

    public SavedAddressService(SavedAddressRepository savedAddressRepository, UserRepository userRepository) {
        this.savedAddressRepository = savedAddressRepository;
        this.userRepository = userRepository;
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    @Transactional
    public SavedAddressResponse createAddress(Long userId, SavedAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        validateCoordinates(request.getLatitude(), request.getLongitude());

        // If this is set as default, unset other default addresses
        if (request.isDefault()) {
            Optional<SavedAddress> existingDefault = savedAddressRepository.findByUserAndIsDefaultTrue(user);
            if (existingDefault.isPresent()) {
                existingDefault.get().setDefault(false);
                savedAddressRepository.save(existingDefault.get());
            }
        }

        SavedAddress savedAddress = new SavedAddress(
                user,
                request.getLabel(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude(),
                request.isDefault()
        );

        SavedAddress saved = savedAddressRepository.save(savedAddress);
        return mapToResponse(saved);
    }

    public List<SavedAddressResponse> getAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<SavedAddress> addresses = savedAddressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user);
        return addresses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SavedAddressResponse getAddress(Long userId, Long addressId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        SavedAddress address = savedAddressRepository.findByUserAndId(user, addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found"));

        return mapToResponse(address);
    }

    @Transactional
    public SavedAddressResponse updateAddress(Long userId, Long addressId, SavedAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        SavedAddress address = savedAddressRepository.findByUserAndId(user, addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found"));

        validateCoordinates(request.getLatitude(), request.getLongitude());

        // If setting as default, unset other default addresses
        if (request.isDefault() && !address.isDefault()) {
            Optional<SavedAddress> existingDefault = savedAddressRepository.findByUserAndIsDefaultTrue(user);
            if (existingDefault.isPresent() && !existingDefault.get().getId().equals(addressId)) {
                existingDefault.get().setDefault(false);
                savedAddressRepository.save(existingDefault.get());
            }
        }

        address.setLabel(request.getLabel());
        address.setAddress(request.getAddress());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setDefault(request.isDefault());

        SavedAddress updated = savedAddressRepository.save(address);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        SavedAddress address = savedAddressRepository.findByUserAndId(user, addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address not found"));

        savedAddressRepository.delete(address);
    }

    private SavedAddressResponse mapToResponse(SavedAddress address) {
        return new SavedAddressResponse(
                address.getId(),
                address.getLabel(),
                address.getAddress(),
                address.getLatitude(),
                address.getLongitude(),
                address.isDefault(),
                address.getCreatedAt()
        );
    }
}

