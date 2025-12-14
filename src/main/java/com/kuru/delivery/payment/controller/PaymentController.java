package com.kuru.delivery.payment.controller;

import com.kuru.delivery.common.util.RoleValidationUtil;
import com.kuru.delivery.payment.dto.InitializePaymentRequest;
import com.kuru.delivery.payment.dto.InitializePaymentResponse;
import com.kuru.delivery.payment.dto.PaymentResponse;
import com.kuru.delivery.payment.dto.VerifyPaymentRequest;
import com.kuru.delivery.payment.model.PaymentTransaction;
import com.kuru.delivery.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;
    private final com.kuru.delivery.payment.repository.PaymentTransactionRepository paymentRepository;
    private final com.kuru.delivery.user.repository.UserRepository userRepository;

    public PaymentController(
            PaymentService paymentService,
            com.kuru.delivery.payment.repository.PaymentTransactionRepository paymentRepository,
            com.kuru.delivery.user.repository.UserRepository userRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> initializePayment(@Valid @RequestBody InitializePaymentRequest request) {
        try {
            Long customerId = getCurrentUserId();
            Map<String, String> result = paymentService.initializePayment(request.getOrderId(), customerId);
            
            InitializePaymentResponse response = new InitializePaymentResponse(
                    result.get("checkoutUrl"),
                    result.get("txRef")
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error initializing payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error initializing payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to initialize payment"));
        }
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        try {
            // CRITICAL SECURITY: Get authenticated user ID for ownership validation
            Long customerId = getCurrentUserId();
            
            PaymentTransaction transaction = paymentService.verifyPayment(request.getTxRef(), customerId);
            PaymentResponse response = mapToPaymentResponse(transaction);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment verified successfully",
                    "payment", response
            ));
        } catch (com.kuru.delivery.common.exception.PaymentSecurityException e) {
            logger.error("Payment security violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (RuntimeException e) {
            logger.error("Error verifying payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (Exception e) {
            logger.error("Unexpected error verifying payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to verify payment"
                    ));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody Map<String, Object> webhookPayload,
            @RequestHeader(value = "X-Chapa-Signature", required = false) String signature,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            logger.info("Received webhook with signature: {}", signature != null ? "present" : "missing");
            
            // SECURITY: Verify webhook signature before processing
            if (!paymentService.verifyWebhookSignature(webhookPayload, signature, request)) {
                logger.error("SECURITY ALERT: Invalid webhook signature. Rejecting webhook request.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "Invalid webhook signature"));
            }
            
            paymentService.handleWebhook(webhookPayload);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Webhook processed"));
        } catch (com.kuru.delivery.common.exception.PaymentSecurityException e) {
            logger.error("Payment security violation in webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllPayments(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String txRef,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        try {
            RoleValidationUtil.validateAdminRole(userRepository);
            
            Pageable pageable = PageRequest.of(page, size, 
                    Sort.by(Sort.Direction.fromString(sortDir), sortBy));

            Page<PaymentTransaction> payments;
            if (orderId != null) {
                List<PaymentTransaction> paymentList = paymentRepository.findByOrderId(orderId);
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), paymentList.size());
                List<PaymentTransaction> pageContent = paymentList.subList(start, end);
                payments = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, paymentList.size());
            } else if (status != null) {
                com.kuru.delivery.payment.model.PaymentStatus paymentStatus = 
                        com.kuru.delivery.payment.model.PaymentStatus.valueOf(status.toUpperCase());
                List<PaymentTransaction> paymentList = paymentRepository.findByStatus(paymentStatus);
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), paymentList.size());
                List<PaymentTransaction> pageContent = paymentList.subList(start, end);
                payments = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, paymentList.size());
            } else if (txRef != null) {
                PaymentTransaction payment = paymentRepository.findByTxRef(txRef).orElse(null);
                if (payment != null) {
                    payments = new org.springframework.data.domain.PageImpl<>(List.of(payment), pageable, 1);
                } else {
                    payments = Page.empty(pageable);
                }
            } else {
                payments = paymentRepository.findAll(pageable);
            }

            List<PaymentResponse> responseList = payments.getContent().stream()
                    .map(this::mapToPaymentResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("payments", responseList);
            response.put("currentPage", payments.getNumber());
            response.put("totalPages", payments.getTotalPages());
            response.put("totalItems", payments.getTotalElements());
            response.put("pageSize", payments.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching payments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch payments"));
        }
    }

    @GetMapping("/admin/{txRef}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPaymentByTxRef(@PathVariable String txRef) {
        try {
            RoleValidationUtil.validateAdminRole(userRepository);
            
            PaymentTransaction transaction = paymentRepository.findByTxRef(txRef)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));
            
            PaymentResponse response = mapToPaymentResponse(transaction);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch payment"));
        }
    }

    private PaymentResponse mapToPaymentResponse(PaymentTransaction transaction) {
        PaymentResponse response = new PaymentResponse();
        response.setId(transaction.getId());
        response.setOrderId(transaction.getOrderId());
        response.setTxRef(transaction.getTxRef());
        response.setChapaReference(transaction.getChapaReference());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setStatus(transaction.getStatus());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email))
                .getId();
    }
}

