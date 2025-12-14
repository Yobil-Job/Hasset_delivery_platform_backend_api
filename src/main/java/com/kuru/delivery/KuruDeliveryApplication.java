package com.kuru.delivery;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KuruDeliveryApplication {

	private static final Logger logger = LoggerFactory.getLogger(KuruDeliveryApplication.class);

	public static void main(String[] args) {
		loadEnvFile();
		SpringApplication.run(KuruDeliveryApplication.class, args);
	}

	private static void loadEnvFile() {
		try {
			Dotenv dotenv = Dotenv.configure()
					.directory("./")
					.ignoreIfMissing()
					.load();

			int loadedCount = 0;
			for (var entry : dotenv.entries()) {
				String key = entry.getKey();
				String value = entry.getValue();
				
				if (System.getenv(key) == null && System.getProperty(key) == null) {
					System.setProperty(key, value);
					loadedCount++;
					logger.debug("Loaded environment variable from .env: {} (length: {})", key, value != null ? value.length() : 0);
				} else {
					logger.debug("Skipping .env variable {} (already set)", key);
				}
			}

			logger.info("✅ Successfully loaded {} environment variables from .env file", loadedCount);
			
			// SECURITY: Only log whether keys are configured, never expose any part of the keys
			String chapaSecret = System.getProperty("CHAPA_SECRET");
			String chapaPublic = System.getProperty("CHAPA_PUBLIC");
			if (chapaSecret != null && !chapaSecret.trim().isEmpty()) {
				logger.info("✅ CHAPA_SECRET is configured");
			} else {
				logger.warn("⚠️  CHAPA_SECRET is NOT configured! Please set CHAPA_SECRET in your .env file.");
			}
			if (chapaPublic != null && !chapaPublic.trim().isEmpty()) {
				logger.info("✅ CHAPA_PUBLIC is configured");
			} else {
				logger.warn("⚠️  CHAPA_PUBLIC is NOT configured! Please set CHAPA_PUBLIC in your .env file.");
			}
		} catch (Exception e) {
			logger.error("❌ Could not load .env file: {}. Using system environment variables or defaults.", e.getMessage(), e);
		}
	}
}


