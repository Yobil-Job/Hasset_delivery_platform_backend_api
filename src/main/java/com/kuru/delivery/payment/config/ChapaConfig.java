package com.kuru.delivery.payment.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChapaConfig {

    private static final Logger logger = LoggerFactory.getLogger(ChapaConfig.class);

    @Value("${chapa.secret}")
    private String secretKey;

    @Value("${chapa.public}")
    private String publicKey;

    @Value("${chapa.mode:SANDBOX}")
    private String mode;

    @Value("${chapa.base.url:https://api.chapa.co/v1}")
    private String baseUrl;

    @Value("${payment.return.url}")
    private String returnUrl;

    @Value("${payment.notify.url}")
    private String notifyUrl;

    @PostConstruct
    public void init() {
        // SECURITY: Only log whether keys are configured, never expose any part of the keys
        if (secretKey == null || secretKey.trim().isEmpty()) {
            logger.error("⚠️  Chapa secret key is NOT configured! Please set CHAPA_SECRET in your .env file.");
        } else {
            logger.info("✅ Chapa secret key is configured");
        }
        
        if (publicKey == null || publicKey.trim().isEmpty()) {
            logger.warn("⚠️  Chapa public key is NOT configured! Please set CHAPA_PUBLIC in your .env file.");
        } else {
            logger.info("✅ Chapa public key is configured");
        }
        
        logger.info("Chapa mode: {}", mode);
        logger.info("Chapa base URL: {}", baseUrl);
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getMode() {
        return mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public boolean isSandbox() {
        return "SANDBOX".equalsIgnoreCase(mode);
    }
}

