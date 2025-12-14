package com.kuru.delivery.faq.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kuru.delivery.faq.model.FAQ;

@Repository
public interface FAQRepository extends JpaRepository<FAQ, Long> {
    
    List<FAQ> findByIsActiveTrueOrderByCategoryAscDisplayOrderAsc();
    
    List<FAQ> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(String category);
    
    List<FAQ> findAllByOrderByCategoryAscDisplayOrderAsc();
    
    List<FAQ> findByCategoryOrderByDisplayOrderAsc(String category);
    
    boolean existsByQuestionIgnoreCase(String question);
}

