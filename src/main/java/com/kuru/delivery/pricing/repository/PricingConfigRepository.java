package com.kuru.delivery.pricing.repository;

import com.kuru.delivery.pricing.model.PricingConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PricingConfigRepository extends JpaRepository<PricingConfiguration, Long> {
}
