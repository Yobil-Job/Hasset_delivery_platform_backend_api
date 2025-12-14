package com.kuru.delivery.payment.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kuru.delivery.payment.config.ChapaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class ChapaApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ChapaApiClient.class);
    private final RestTemplate restTemplate;
    private final ChapaConfig chapaConfig;

    public ChapaApiClient(ChapaConfig chapaConfig) {
        this.chapaConfig = chapaConfig;
        this.restTemplate = new RestTemplate();
    }

    public ChapaInitializeResponse initializePayment(String txRef, BigDecimal amount, String currency, 
                                                     String email, String firstName, String lastName,
                                                     String phoneNumber, String callbackUrl, String returnUrl) {
        try {
            String secretKey = chapaConfig.getSecretKey();
            if (secretKey == null || secretKey.trim().isEmpty()) {
                logger.error("Chapa secret key is not configured. Please set CHAPA_SECRET in your .env file.");
                throw new RuntimeException("Payment service is not configured. Please contact support.");
            }
            
            secretKey = secretKey.trim();
            logger.debug("Using Chapa secret key for payment initialization");
            
            String url = chapaConfig.getBaseUrl() + "/transaction/initialize";
            logger.info("Initializing payment with Chapa API: {}", url);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", amount.toString());
            requestBody.put("currency", currency);
            requestBody.put("email", email);
            requestBody.put("first_name", firstName);
            requestBody.put("last_name", lastName);
            
            // Phone number is required by Chapa - ensure it's not empty
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new RuntimeException("Phone number is required for payment");
            }
            requestBody.put("phone_number", phoneNumber);
            
            requestBody.put("tx_ref", txRef);
            requestBody.put("callback_url", callbackUrl);
            requestBody.put("return_url", returnUrl);
            // Customization for Chapa checkout page
            Map<String, String> customization = new HashMap<>();
            customization.put("title", "Hasset Delivery");
            customization.put("description", "Delivery Payment");
            requestBody.put("customization", customization);
            
            // Note: Chapa automatically shows all available payment methods (CBE, Awash, Telebirr, etc.)
            // on their hosted checkout page. In SANDBOX mode, only test methods are shown.
            // In LIVE mode, all real payment methods will be available.

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(secretKey);
            
            logger.debug("Request headers - Content-Type: {}, Authorization: Bearer {}...", 
                headers.getContentType(), 
                secretKey.substring(0, Math.min(10, secretKey.length())));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.info("Initializing Chapa payment for txRef: {}", txRef);
            ResponseEntity<ChapaInitializeResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, request, ChapaInitializeResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Chapa payment initialized successfully. Status: {}", response.getBody().getStatus());
                return response.getBody();
            } else {
                logger.error("Failed to initialize Chapa payment. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to initialize payment with Chapa");
            }
        } catch (Exception e) {
            logger.error("Error initializing Chapa payment: {}", e.getMessage(), e);
            throw new RuntimeException("Error initializing payment: " + e.getMessage(), e);
        }
    }

    public ChapaVerifyResponse verifyPayment(String txRef) {
        try {
            String url = chapaConfig.getBaseUrl() + "/transaction/verify/" + txRef;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(chapaConfig.getSecretKey());

            HttpEntity<Void> request = new HttpEntity<>(headers);

            logger.info("Verifying Chapa payment for txRef: {}", txRef);
            ResponseEntity<ChapaVerifyResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, request, ChapaVerifyResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Chapa payment verified. Status: {}", response.getBody().getStatus());
                return response.getBody();
            } else {
                logger.error("Failed to verify Chapa payment. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to verify payment with Chapa");
            }
        } catch (Exception e) {
            logger.error("Error verifying Chapa payment: {}", e.getMessage(), e);
            throw new RuntimeException("Error verifying payment: " + e.getMessage(), e);
        }
    }

    // Inner classes for Chapa API responses
    public static class ChapaInitializeResponse {
        @JsonProperty("status")
        private String status;

        @JsonProperty("message")
        private String message;

        @JsonProperty("data")
        private ChapaData data;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public ChapaData getData() {
            return data;
        }

        public void setData(ChapaData data) {
            this.data = data;
        }

        public static class ChapaData {
            @JsonProperty("checkout_url")
            private String checkoutUrl;

            public String getCheckoutUrl() {
                return checkoutUrl;
            }

            public void setCheckoutUrl(String checkoutUrl) {
                this.checkoutUrl = checkoutUrl;
            }
        }
    }

    public static class ChapaVerifyResponse {
        @JsonProperty("status")
        private String status;

        @JsonProperty("message")
        private String message;

        @JsonProperty("data")
        private ChapaVerifyData data;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public ChapaVerifyData getData() {
            return data;
        }

        public void setData(ChapaVerifyData data) {
            this.data = data;
        }

        public static class ChapaVerifyData {
            @JsonProperty("amount")
            private BigDecimal amount;

            @JsonProperty("currency")
            private String currency;

            @JsonProperty("tx_ref")
            private String txRef;

            @JsonProperty("flw_ref")
            private String flwRef;

            @JsonProperty("status")
            private String status;

            @JsonProperty("customer")
            private ChapaCustomer customer;

            public BigDecimal getAmount() {
                return amount;
            }

            public void setAmount(BigDecimal amount) {
                this.amount = amount;
            }

            public String getCurrency() {
                return currency;
            }

            public void setCurrency(String currency) {
                this.currency = currency;
            }

            public String getTxRef() {
                return txRef;
            }

            public void setTxRef(String txRef) {
                this.txRef = txRef;
            }

            public String getFlwRef() {
                return flwRef;
            }

            public void setFlwRef(String flwRef) {
                this.flwRef = flwRef;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public ChapaCustomer getCustomer() {
                return customer;
            }

            public void setCustomer(ChapaCustomer customer) {
                this.customer = customer;
            }

            public static class ChapaCustomer {
                @JsonProperty("email")
                private String email;

                @JsonProperty("name")
                private String name;

                public String getEmail() {
                    return email;
                }

                public void setEmail(String email) {
                    this.email = email;
                }

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }
            }
        }
    }
}

