package com.kuru.delivery.config;

import com.kuru.delivery.pricing.model.PricingConfiguration;
import com.kuru.delivery.pricing.model.ServiceOffering;
import com.kuru.delivery.pricing.repository.PricingConfigRepository;
import com.kuru.delivery.pricing.repository.ServiceOfferingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final PricingConfigRepository pricingConfigRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;

    public DataInitializer(PricingConfigRepository pricingConfigRepository,
                           ServiceOfferingRepository serviceOfferingRepository) {
        this.pricingConfigRepository = pricingConfigRepository;
        this.serviceOfferingRepository = serviceOfferingRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Initialize Pricing Configuration
        if (pricingConfigRepository.count() == 0) {
            PricingConfiguration config = new PricingConfiguration();
            config.setBaseFee(50.0);
            config.setDistanceRatePerKm(10.0);
            config.setFreeWeightLimit(5.0);
            config.setAdditionalWeightFeePerKg(5.0);
            pricingConfigRepository.save(config);
            logger.info("Initialized Pricing Configuration");
        }

        // Initialize Service Offerings
        if (serviceOfferingRepository.count() == 0) {
            ServiceOffering standard = new ServiceOffering();
            standard.setTitle("Standard Delivery");
            standard.setDescription("Reliable delivery within 24 hours");
            standard.setPriceText("Standard Rate");
            standard.setMultiplier(0.0); // No extra fee
            standard.setFeatures(Arrays.asList("Tracking", "Insurance"));
            standard.setImageUrl("https://images.unsplash.com/photo-1616401784845-180886ba9ca8?auto=format&fit=crop&q=80&w=1000");
            standard.setGradient("from-blue-500 to-cyan-500");
            standard.setIcon("Truck");

            ServiceOffering express = new ServiceOffering();
            express.setTitle("Express Delivery");
            express.setDescription("Fast delivery within 4 hours");
            express.setPriceText("1.5x Rate");
            express.setMultiplier(0.5); // +50%
            express.setFeatures(Arrays.asList("Priority Handling", "Real-time Tracking", "SMS Updates"));
            express.setImageUrl("https://images.unsplash.com/photo-1580674684081-7617fbf3d745?auto=format&fit=crop&q=80&w=1000");
            express.setGradient("from-orange-500 to-red-500");
            express.setIcon("Zap");

            ServiceOffering vip = new ServiceOffering();
            vip.setTitle("VIP Secure");
            vip.setDescription("Direct delivery with high security");
            vip.setPriceText("2x Rate");
            vip.setMultiplier(1.0); // +100%
            vip.setFeatures(Arrays.asList("Direct Route", "High Value Insurance", "Dedicated Support"));
            vip.setImageUrl("https://images.unsplash.com/photo-1586864387967-d02ef85d93e8?auto=format&fit=crop&q=80&w=1000");
            vip.setGradient("from-purple-500 to-pink-500");
            vip.setIcon("Shield");

            serviceOfferingRepository.saveAll(Arrays.asList(standard, express, vip));
            logger.info("Initialized Service Offerings");
        }
    }
}
