package com.kuru.delivery.order.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kuru.delivery.driver.model.Driver;
import com.kuru.delivery.driver.repository.DriverRepository;
import com.kuru.delivery.order.dto.DeliveryProofResponse;
import com.kuru.delivery.order.dto.UploadDeliveryProofResponse;
import com.kuru.delivery.order.model.DeliveryProof;
import com.kuru.delivery.order.model.Order;
import com.kuru.delivery.order.repository.DeliveryProofRepository;
import com.kuru.delivery.order.repository.OrderRepository;
import com.kuru.delivery.user.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class DeliveryProofService {

    private final DeliveryProofRepository deliveryProofRepository;
    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.upload.dir:uploads/delivery-proofs}")
    private String uploadDir;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    public DeliveryProofService(
            DeliveryProofRepository deliveryProofRepository,
            OrderRepository orderRepository,
            DriverRepository driverRepository,
            UserRepository userRepository,
            CloudinaryService cloudinaryService,
            SimpMessagingTemplate messagingTemplate) {
        this.deliveryProofRepository = deliveryProofRepository;
        this.orderRepository = orderRepository;
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public UploadDeliveryProofResponse uploadProof(String orderNumber, Long driverUserId, MultipartFile file) throws IOException {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        Driver driver = driverRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new EntityNotFoundException("Driver not found"));
        if (order.getDriver() == null || !order.getDriver().getId().equals(driver.getId())) {
            throw new IllegalStateException("You can only upload proof for orders assigned to you");
        }

        validateImageFile(file);

        String imageUrl;
        if (cloudinaryService.isConfigured()) {
            try {
                imageUrl = cloudinaryService.uploadImage(file, "delivery-proofs");
            } catch (Exception e) {
                throw new IOException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
            }
        } else {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(filename);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            imageUrl = baseUrl + "/uploads/delivery-proofs/" + filename;
        }

        DeliveryProof proof = new DeliveryProof();
        proof.setOrder(order);
        proof.setDriver(driver);
        proof.setImageUrl(imageUrl);
        proof.setUploadedAt(LocalDateTime.now());

        DeliveryProof saved = deliveryProofRepository.save(proof);

        String driverName = driver.getUser() != null
                ? driver.getUser().getFirstname() + " " + driver.getUser().getLastname()
                : "Driver";
        DeliveryProofResponse proofResponse = new DeliveryProofResponse(
                saved.getId(),
                saved.getImageUrl(),
                saved.getUploadedAt(),
                driverName
        );

        messagingTemplate.convertAndSend("/topic/delivery-proof/" + orderNumber, proofResponse);

        return new UploadDeliveryProofResponse(
                saved.getId(),
                saved.getImageUrl(),
                saved.getUploadedAt()
        );
    }

    public List<DeliveryProofResponse> getProofs(String orderNumber, Long userId, boolean isAdmin) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (!isAdmin) {
            boolean isCustomer = order.getCustomerId().equals(userId);
            boolean isDriver = order.getDriver() != null && 
                              order.getDriver().getUser() != null &&
                              order.getDriver().getUser().getId().equals(userId);
            
            if (!isCustomer && !isDriver) {
                throw new IllegalStateException("You can only view delivery proofs for your own orders");
            }
        }

        List<DeliveryProof> proofs = deliveryProofRepository.findByOrderOrderByUploadedAtDesc(order);
        return proofs.stream()
                .map(proof -> {
                    String driverName = proof.getDriver().getUser() != null
                            ? proof.getDriver().getUser().getFirstname() + " " + proof.getDriver().getUser().getLastname()
                            : "Driver";
                    return new DeliveryProofResponse(
                            proof.getId(),
                            proof.getImageUrl(),
                            proof.getUploadedAt(),
                            driverName
                    );
                })
                .collect(Collectors.toList());
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size must be less than 5MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.toLowerCase();
            if (!extension.endsWith(".jpg") && !extension.endsWith(".jpeg") 
                && !extension.endsWith(".png") && !extension.endsWith(".gif")) {
                throw new IllegalArgumentException("Only JPG, JPEG, PNG, and GIF images are allowed");
            }
        }

        // SECURITY: Validate file magic bytes (file signature) to prevent file type spoofing
        validateFileMagicBytes(file);
    }

    // SECURITY: Validate file magic bytes to prevent file type spoofing
    private void validateFileMagicBytes(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < 4) {
                throw new IllegalArgumentException("File is too small to be a valid image");
            }

            boolean isValidImage = false;
            
            if (fileBytes.length >= 3 && 
                fileBytes[0] == (byte) 0xFF && 
                fileBytes[1] == (byte) 0xD8 && 
                fileBytes[2] == (byte) 0xFF) {
                isValidImage = true;
            }
            else if (fileBytes.length >= 4 && 
                     fileBytes[0] == (byte) 0x89 && 
                     fileBytes[1] == (byte) 0x50 && 
                     fileBytes[2] == (byte) 0x4E && 
                     fileBytes[3] == (byte) 0x47) {
                isValidImage = true;
            }
            else if (fileBytes.length >= 4 && 
                     fileBytes[0] == (byte) 0x47 && 
                     fileBytes[1] == (byte) 0x49 && 
                     fileBytes[2] == (byte) 0x46 && 
                     fileBytes[3] == (byte) 0x38) {
                isValidImage = true;
            }

            if (!isValidImage) {
                throw new IllegalArgumentException("File does not appear to be a valid image. File signature validation failed.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to validate file: " + e.getMessage());
        }
    }
}

