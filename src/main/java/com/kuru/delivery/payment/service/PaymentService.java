package com.kuru.delivery.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuru.delivery.order.model.Order;
import com.kuru.delivery.order.model.OrderStatus;
import com.kuru.delivery.order.repository.OrderRepository;
import com.kuru.delivery.payment.config.ChapaConfig;
import com.kuru.delivery.payment.integration.ChapaApiClient;
import com.kuru.delivery.payment.model.PaymentStatus;
import com.kuru.delivery.payment.model.PaymentTransaction;
import com.kuru.delivery.payment.repository.PaymentTransactionRepository;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentTransactionRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ChapaApiClient chapaApiClient;
    private final ChapaConfig chapaConfig;
    private final ObjectMapper objectMapper;

    public PaymentService(
            PaymentTransactionRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            ChapaApiClient chapaApiClient,
            ChapaConfig chapaConfig) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.chapaApiClient = chapaApiClient;
        this.chapaConfig = chapaConfig;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public Map<String, String> initializePayment(Long orderId, Long customerId) {
        logger.info("Initializing payment for orderId: {}, customerId: {}", orderId, customerId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // CRITICAL SECURITY: Validate order ownership before payment
        if (!order.getCustomerId().equals(customerId)) {
            logger.error("SECURITY ALERT: Payment initialization attempted for unauthorized order. " +
                    "OrderId: {}, Attempted by customerId: {}, Actual owner: {}", 
                    orderId, customerId, order.getCustomerId());
            throw new com.kuru.delivery.common.exception.PaymentSecurityException(
                "You do not have permission to pay for this order"
            );
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Order is already paid");
        }
        PaymentTransaction existingPayment = paymentRepository
                .findByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS)
                .stream()
                .findFirst()
                .orElse(null);

        if (existingPayment != null) {
            throw new RuntimeException("Order already has a successful payment");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // CRITICAL SECURITY: Generate unique tx_ref with secure format
        String txRef = "ORDER-" + orderId + "-" + UUID.randomUUID().toString();

        // CRITICAL SECURITY: Amount is ALWAYS taken from order, NEVER from frontend
        BigDecimal orderAmount = order.getPrice();
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("SECURITY ALERT: Invalid order amount. OrderId: {}, Amount: {}", orderId, orderAmount);
            throw new RuntimeException("Invalid order amount. Cannot process payment.");
        }
        
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId(orderId);
        transaction.setTxRef(txRef);
        // CRITICAL SECURITY: Amount is from order.getPrice() only - never from frontend
        transaction.setAmount(orderAmount);
        transaction.setCurrency("ETB");
        transaction.setStatus(PaymentStatus.PENDING);
        
        logger.info("Payment transaction created. OrderId: {}, Amount: {}, txRef: {}", 
                orderId, orderAmount, txRef);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderNumber", order.getOrderNumber());
        metadata.put("customerEmail", customer.getEmail());
        metadata.put("customerName", customer.getFirstname() + " " + customer.getLastname());
        try {
            transaction.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            logger.warn("Failed to serialize metadata: {}", e.getMessage());
        }

        transaction = paymentRepository.save(transaction);
        logger.info("Created payment transaction with txRef: {}", txRef);

        // Initialize payment with Chapa
        try {
            String callbackUrl = chapaConfig.getNotifyUrl();
            // Chapa uses tx_ref parameter in redirect, so we include it in return URL
            String returnUrl = chapaConfig.getReturnUrl() + "?tx_ref=" + txRef;

            // Validate phone number - Chapa requires a valid phone number
            String phoneNumber = customer.getPhoneNumber();
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new RuntimeException("Phone number is required for payment. Please update your profile with a valid phone number.");
            }
            
            // Clean phone number (remove spaces, dashes, etc.)
            phoneNumber = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
            
            // Ensure phone number starts with country code if not present
            // Chapa expects format: +251XXXXXXXXX or 251XXXXXXXXX
            if (!phoneNumber.startsWith("+") && !phoneNumber.startsWith("251")) {
                // If it starts with 0, replace with 251
                if (phoneNumber.startsWith("0")) {
                    phoneNumber = "251" + phoneNumber.substring(1);
                } else if (phoneNumber.length() == 9) {
                    // If it's 9 digits, assume it's missing country code
                    phoneNumber = "251" + phoneNumber;
                }
            }
            
            // Add + prefix if not present
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = "+" + phoneNumber;
            }
            
            ChapaApiClient.ChapaInitializeResponse response = chapaApiClient.initializePayment(
                    txRef,
                    order.getPrice(),
                    "ETB",
                    customer.getEmail(),
                    customer.getFirstname(),
                    customer.getLastname(),
                    phoneNumber,
                    callbackUrl,
                    returnUrl
            );

            if (response.getStatus() != null && response.getStatus().equals("success") 
                    && response.getData() != null && response.getData().getCheckoutUrl() != null) {
                
                Map<String, String> result = new HashMap<>();
                result.put("checkoutUrl", response.getData().getCheckoutUrl());
                result.put("txRef", txRef);
                
                logger.info("Payment initialized successfully. Checkout URL generated for txRef: {}", txRef);
                return result;
            } else {
                // Update transaction status to failed
                transaction.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(transaction);
                throw new RuntimeException("Failed to initialize payment: " + 
                        (response.getMessage() != null ? response.getMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            logger.error("Error initializing payment with Chapa: {}", e.getMessage(), e);
            transaction.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(transaction);
            throw new RuntimeException("Error initializing payment: " + e.getMessage(), e);
        }
    }

    @Transactional
    public PaymentTransaction verifyPayment(String txRef, Long customerId) {
        logger.info("Verifying payment for txRef: {}, customerId: {}", txRef, customerId);

        PaymentTransaction transaction = paymentRepository.findByTxRef(txRef)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found"));

        // CRITICAL SECURITY: Validate order ownership before verification
        Order order = orderRepository.findById(transaction.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.getCustomerId().equals(customerId)) {
            logger.error("SECURITY ALERT: Payment verification attempted for unauthorized order. " +
                    "txRef: {}, Attempted by customerId: {}, Actual owner: {}", 
                    txRef, customerId, order.getCustomerId());
            throw new com.kuru.delivery.common.exception.PaymentSecurityException(
                "You do not have permission to verify this payment"
            );
        }

        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            logger.info("Payment already verified for txRef: {}", txRef);
            return transaction;
        }
        try {
            ChapaApiClient.ChapaVerifyResponse verifyResponse = chapaApiClient.verifyPayment(txRef);

            if (verifyResponse.getStatus() != null && verifyResponse.getStatus().equals("success")
                    && verifyResponse.getData() != null) {
                
                ChapaApiClient.ChapaVerifyResponse.ChapaVerifyData data = verifyResponse.getData();

                // CRITICAL SECURITY: Validate amount matches order amount (fraud detection)
                BigDecimal chapaAmount = data.getAmount();
                BigDecimal orderAmount = order.getPrice();

                if (chapaAmount.compareTo(orderAmount) != 0) {
                    // FRAUD ATTEMPT DETECTED: Amount mismatch
                    logger.error("FRAUD ALERT: Amount mismatch detected for txRef: {}. " +
                            "Chapa Amount: {}, Order Amount: {}, OrderId: {}, CustomerId: {}", 
                            txRef, chapaAmount, orderAmount, order.getId(), order.getCustomerId());
                    
                    // Mark transaction as failed
                    transaction.setStatus(PaymentStatus.FAILED);
                    
                    // Store fraud indicator in metadata
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metadata = transaction.getMetadata() != null 
                                ? objectMapper.readValue(transaction.getMetadata(), Map.class)
                                : new HashMap<>();
                        metadata.put("fraudDetected", true);
                        metadata.put("fraudReason", "Amount mismatch");
                        metadata.put("chapaAmount", chapaAmount.toString());
                        metadata.put("orderAmount", orderAmount.toString());
                        metadata.put("detectedAt", System.currentTimeMillis());
                        transaction.setMetadata(objectMapper.writeValueAsString(metadata));
                    } catch (Exception e) {
                        logger.warn("Failed to store fraud metadata: {}", e.getMessage());
                    }
                    
                    paymentRepository.save(transaction);
                    
                    // DO NOT mark order as paid - this is a fraud attempt
                    throw new com.kuru.delivery.common.exception.PaymentSecurityException(
                        "Payment amount mismatch detected. This transaction has been flagged for review."
                    );
                }

                // Validate currency
                if (!"ETB".equalsIgnoreCase(data.getCurrency())) {
                    logger.error("Currency mismatch for txRef: {}. Expected: ETB, Got: {}", 
                            txRef, data.getCurrency());
                    transaction.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(transaction);
                    throw new RuntimeException("Payment currency mismatch");
                }

                // Check payment status from Chapa
                String chapaStatus = data.getStatus();
                if ("successful".equalsIgnoreCase(chapaStatus) || "success".equalsIgnoreCase(chapaStatus)) {
                    // Update transaction
                    transaction.setStatus(PaymentStatus.SUCCESS);
                    transaction.setChapaReference(data.getFlwRef());

                    // Update metadata
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metadata = transaction.getMetadata() != null 
                                ? objectMapper.readValue(transaction.getMetadata(), Map.class)
                                : new HashMap<>();
                        metadata.put("chapaStatus", chapaStatus);
                        metadata.put("verifiedAt", System.currentTimeMillis());
                        transaction.setMetadata(objectMapper.writeValueAsString(metadata));
                    } catch (Exception e) {
                        logger.warn("Failed to update metadata: {}", e.getMessage());
                    }

                    transaction = paymentRepository.save(transaction);

                    if (order.getStatus() != OrderStatus.PAID) {
                        order.setStatus(OrderStatus.PAID);
                        orderRepository.save(order);
                        logger.info("Order {} marked as PAID", order.getOrderNumber());
                    }

                    logger.info("Payment verified successfully for txRef: {}", txRef);
                    return transaction;
                } else {
                    // Payment failed or pending
                    transaction.setStatus(PaymentStatus.FAILED);
                    transaction.setChapaReference(data.getFlwRef());
                    paymentRepository.save(transaction);
                    logger.warn("Payment verification failed. Chapa status: {} for txRef: {}", chapaStatus, txRef);
                    throw new RuntimeException("Payment verification failed. Status: " + chapaStatus);
                }
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(transaction);
                throw new RuntimeException("Payment verification failed: " + 
                        (verifyResponse.getMessage() != null ? verifyResponse.getMessage() : "Unknown error"));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error verifying payment with Chapa: {}", e.getMessage(), e);
            transaction.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(transaction);
            throw new RuntimeException("Error verifying payment: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void handleWebhook(Map<String, Object> webhookPayload) {
        logger.info("Received webhook: {}", webhookPayload);

        // Extract tx_ref or chapa_reference from webhook
        String txRef = null;
        String chapaRef = null;

        if (webhookPayload.containsKey("tx_ref")) {
            txRef = (String) webhookPayload.get("tx_ref");
        }

        if (webhookPayload.containsKey("flw_ref")) {
            chapaRef = (String) webhookPayload.get("flw_ref");
        }

        if (txRef == null && chapaRef == null) {
            logger.error("Webhook missing both tx_ref and flw_ref");
            throw new RuntimeException("Invalid webhook: missing transaction reference");
        }

        PaymentTransaction transaction = null;
        if (txRef != null) {
            transaction = paymentRepository.findByTxRef(txRef).orElse(null);
        }
        if (transaction == null && chapaRef != null) {
            transaction = paymentRepository.findByChapaReference(chapaRef).orElse(null);
        }

        if (transaction == null) {
            logger.error("Transaction not found for webhook. txRef: {}, chapaRef: {}", txRef, chapaRef);
            throw new RuntimeException("Transaction not found");
        }

        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            logger.info("Payment already successful for txRef: {}. Ignoring webhook.", transaction.getTxRef());
            return;
        }

        // CRITICAL SECURITY: Verify payment using Chapa API (do NOT trust webhook body alone)
        BigDecimal webhookAmount = null;
        if (webhookPayload.containsKey("amount")) {
            try {
                Object amountObj = webhookPayload.get("amount");
                if (amountObj instanceof Number) {
                    webhookAmount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
                } else if (amountObj instanceof String) {
                    webhookAmount = new BigDecimal((String) amountObj);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse webhook amount: {}", e.getMessage());
            }
        }
        
        Order order = orderRepository.findById(transaction.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // CRITICAL SECURITY: Validate webhook amount matches order amount (fraud detection)
        if (webhookAmount != null) {
            BigDecimal orderAmount = order.getPrice();
            if (webhookAmount.compareTo(orderAmount) != 0) {
                logger.error("FRAUD ALERT: Webhook amount mismatch detected. " +
                        "txRef: {}, Webhook Amount: {}, Order Amount: {}, OrderId: {}, CustomerId: {}", 
                        transaction.getTxRef(), webhookAmount, orderAmount, order.getId(), order.getCustomerId());
                
                transaction.setStatus(PaymentStatus.FAILED);
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = transaction.getMetadata() != null 
                            ? objectMapper.readValue(transaction.getMetadata(), Map.class)
                            : new HashMap<>();
                    metadata.put("fraudDetected", true);
                    metadata.put("fraudReason", "Webhook amount mismatch");
                    metadata.put("webhookAmount", webhookAmount.toString());
                    metadata.put("orderAmount", orderAmount.toString());
                    metadata.put("detectedAt", System.currentTimeMillis());
                    transaction.setMetadata(objectMapper.writeValueAsString(metadata));
                } catch (Exception e) {
                    logger.warn("Failed to store fraud metadata: {}", e.getMessage());
                }
                
                paymentRepository.save(transaction);
                throw new com.kuru.delivery.common.exception.PaymentSecurityException(
                    "Webhook amount mismatch detected. This transaction has been flagged for review."
                );
            }
        }
        
        // CRITICAL SECURITY: Verify tx_ref belongs to this order
        if (!transaction.getTxRef().startsWith("ORDER-" + order.getId() + "-")) {
            logger.error("FRAUD ALERT: tx_ref tampering detected. " +
                    "txRef: {}, OrderId: {}, Expected prefix: ORDER-{}-", 
                    transaction.getTxRef(), order.getId(), order.getId());
            
            transaction.setStatus(PaymentStatus.FAILED);
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = transaction.getMetadata() != null 
                        ? objectMapper.readValue(transaction.getMetadata(), Map.class)
                        : new HashMap<>();
                metadata.put("fraudDetected", true);
                metadata.put("fraudReason", "tx_ref tampering");
                metadata.put("detectedAt", System.currentTimeMillis());
                transaction.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                logger.warn("Failed to store fraud metadata: {}", e.getMessage());
            }
            paymentRepository.save(transaction);
            
            throw new com.kuru.delivery.common.exception.PaymentSecurityException(
                "Transaction reference tampering detected. This transaction has been flagged for review."
            );
        }
        
        logger.info("Verifying payment from webhook for txRef: {}", transaction.getTxRef());
        verifyPaymentInternal(transaction.getTxRef());
    }
    
    @Transactional
    private void verifyPaymentInternal(String txRef) {
        logger.info("Internal payment verification for txRef: {}", txRef);

        PaymentTransaction transaction = paymentRepository.findByTxRef(txRef)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found"));

        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            logger.info("Payment already verified for txRef: {}", txRef);
            return;
        }
        try {
            ChapaApiClient.ChapaVerifyResponse verifyResponse = chapaApiClient.verifyPayment(txRef);

            if (verifyResponse.getStatus() != null && verifyResponse.getStatus().equals("success")
                    && verifyResponse.getData() != null) {
                
                ChapaApiClient.ChapaVerifyResponse.ChapaVerifyData data = verifyResponse.getData();

                Order order = orderRepository.findById(transaction.getOrderId())
                        .orElseThrow(() -> new RuntimeException("Order not found"));

                // CRITICAL SECURITY: Validate amount matches order amount (fraud detection)
                BigDecimal chapaAmount = data.getAmount();
                BigDecimal orderAmount = order.getPrice();

                if (chapaAmount.compareTo(orderAmount) != 0) {
                    // FRAUD ATTEMPT DETECTED: Amount mismatch
                    logger.error("FRAUD ALERT: Amount mismatch detected for txRef: {}. " +
                            "Chapa Amount: {}, Order Amount: {}, OrderId: {}, CustomerId: {}", 
                            txRef, chapaAmount, orderAmount, order.getId(), order.getCustomerId());
                    
                    // Mark transaction as failed
                    transaction.setStatus(PaymentStatus.FAILED);
                    
                    // Store fraud indicator in metadata
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metadata = transaction.getMetadata() != null 
                                ? objectMapper.readValue(transaction.getMetadata(), Map.class)
                                : new HashMap<>();
                        metadata.put("fraudDetected", true);
                        metadata.put("fraudReason", "Amount mismatch");
                        metadata.put("chapaAmount", chapaAmount.toString());
                        metadata.put("orderAmount", orderAmount.toString());
                        metadata.put("detectedAt", System.currentTimeMillis());
                        transaction.setMetadata(objectMapper.writeValueAsString(metadata));
                    } catch (Exception e) {
                        logger.warn("Failed to store fraud metadata: {}", e.getMessage());
                    }
                    
                    paymentRepository.save(transaction);
                    
                    // DO NOT mark order as paid - this is a fraud attempt
                    throw new com.kuru.delivery.common.exception.PaymentSecurityException(
                        "Payment amount mismatch detected. This transaction has been flagged for review."
                    );
                }

                // Validate currency
                if (!"ETB".equalsIgnoreCase(data.getCurrency())) {
                    logger.error("Currency mismatch for txRef: {}. Expected: ETB, Got: {}", 
                            txRef, data.getCurrency());
                    transaction.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(transaction);
                    throw new RuntimeException("Payment currency mismatch");
                }

                // Check payment status from Chapa
                String chapaStatus = data.getStatus();
                if ("successful".equalsIgnoreCase(chapaStatus) || "success".equalsIgnoreCase(chapaStatus)) {
                    // Update transaction
                    transaction.setStatus(PaymentStatus.SUCCESS);
                    transaction.setChapaReference(data.getFlwRef());

                    // Update metadata
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metadata = transaction.getMetadata() != null 
                                ? objectMapper.readValue(transaction.getMetadata(), Map.class)
                                : new HashMap<>();
                        metadata.put("chapaStatus", chapaStatus);
                        metadata.put("verifiedAt", System.currentTimeMillis());
                        transaction.setMetadata(objectMapper.writeValueAsString(metadata));
                    } catch (Exception e) {
                        logger.warn("Failed to update metadata: {}", e.getMessage());
                    }

                    transaction = paymentRepository.save(transaction);

                    if (order.getStatus() != OrderStatus.PAID) {
                        order.setStatus(OrderStatus.PAID);
                        orderRepository.save(order);
                        logger.info("Order {} marked as PAID via webhook", order.getOrderNumber());
                    }

                    logger.info("Payment verified successfully via webhook for txRef: {}", txRef);
                    return;
                } else {
                    // Payment failed or pending
                    transaction.setStatus(PaymentStatus.FAILED);
                    transaction.setChapaReference(data.getFlwRef());
                    paymentRepository.save(transaction);
                    logger.warn("Payment verification failed. Chapa status: {} for txRef: {}", chapaStatus, txRef);
                    throw new RuntimeException("Payment verification failed. Status: " + chapaStatus);
                }
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(transaction);
                throw new RuntimeException("Payment verification failed: " + 
                        (verifyResponse.getMessage() != null ? verifyResponse.getMessage() : "Unknown error"));
            }
        } catch (com.kuru.delivery.common.exception.PaymentSecurityException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error verifying payment with Chapa: {}", e.getMessage(), e);
            transaction.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(transaction);
            throw new RuntimeException("Error verifying payment: " + e.getMessage(), e);
        }
    }

    public boolean verifyWebhookSignature(Map<String, Object> webhookPayload, String signature, HttpServletRequest request) {
        if (signature == null || signature.trim().isEmpty()) {
            if (chapaConfig.isSandbox()) {
                logger.warn("Webhook signature missing in SANDBOX mode. Allowing for testing purposes.");
                return true; // Allow in sandbox for testing
            }
            logger.error("SECURITY ALERT: Webhook signature missing in production mode!");
            return false;
        }

        try {
            String secretKey = chapaConfig.getSecretKey();
            if (secretKey == null || secretKey.trim().isEmpty()) {
                logger.error("Chapa secret key not configured. Cannot verify webhook signature.");
                return false;
            }

            String payloadJson = objectMapper.writeValueAsString(webhookPayload);
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            String calculatedSignature = hexString.toString();
            
            if (signature.length() != calculatedSignature.length()) {
                logger.error("Webhook signature length mismatch. Expected: {}, Got: {}", 
                    calculatedSignature.length(), signature.length());
                return false;
            }
            
            boolean signaturesMatch = true;
            for (int i = 0; i < signature.length(); i++) {
                if (signature.charAt(i) != calculatedSignature.charAt(i)) {
                    signaturesMatch = false;
                }
            }
            
            if (!signaturesMatch) {
                logger.error("SECURITY ALERT: Webhook signature verification failed!");
                return false;
            }
            
            logger.info("Webhook signature verified successfully");
            return true;
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("HMAC-SHA256 algorithm not available: {}", e.getMessage());
            return false;
        } catch (InvalidKeyException e) {
            logger.error("Invalid secret key for HMAC: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error verifying webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }
}

