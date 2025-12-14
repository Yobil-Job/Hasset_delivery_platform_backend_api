package com.kuru.delivery.driver.service;

import com.kuru.delivery.auth.dto.RegisterRequest;
import com.kuru.delivery.auth.service.AuthEmailService;
import com.kuru.delivery.driver.model.Driver;
import com.kuru.delivery.driver.model.DriverStatus;
import com.kuru.delivery.driver.repository.DriverRepository;
import com.kuru.delivery.user.model.Role;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DriverService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthEmailService authEmailService;

    public DriverService(UserRepository userRepository, DriverRepository driverRepository, 
                        PasswordEncoder passwordEncoder, AuthEmailService authEmailService) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
        this.authEmailService = authEmailService;
    }

    @Transactional
    public void registerDriver(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setRole(Role.DRIVER);

        User savedUser = userRepository.save(user);

        Driver driver = new Driver(savedUser, DriverStatus.PENDING);
        driver.setVehicleType(request.getVehicleType());
        driver.setLicenseNumber(request.getLicenseNumber());
        driverRepository.save(driver);

        // Generate and send verification code
        authEmailService.sendVerificationCode(savedUser);
    }

    public List<Driver> getPendingDrivers() {
        return driverRepository.findByStatus(DriverStatus.PENDING);
    }

    public void approveDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        driver.setStatus(DriverStatus.APPROVED);
        driverRepository.save(driver);
    }

    public void rejectDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        driver.setStatus(DriverStatus.REJECTED);
        driverRepository.save(driver);
    }

    public void suspendDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        driver.setStatus(DriverStatus.SUSPENDED);
        driverRepository.save(driver);
    }

    public java.util.Optional<Driver> getDriverByUserId(Long userId) {
        return driverRepository.findByUserId(userId);
    }
}
