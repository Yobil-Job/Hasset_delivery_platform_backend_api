package com.kuru.delivery.faq.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kuru.delivery.faq.dto.FAQRequest;
import com.kuru.delivery.faq.dto.FAQResponse;
import com.kuru.delivery.faq.model.FAQ;
import com.kuru.delivery.faq.repository.FAQRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FAQService {

    private final FAQRepository faqRepository;
    
    // Get all active FAQs (for public customer UI)
    @Transactional(readOnly = true)
    public List<FAQResponse> getAllActiveFAQs() {
        return faqRepository.findByIsActiveTrueOrderByCategoryAscDisplayOrderAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get all FAQs (for admin UI)
    @Transactional(readOnly = true)
    public List<FAQResponse> getAllFAQs() {
        return faqRepository.findAllByOrderByCategoryAscDisplayOrderAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get FAQ by ID
    @Transactional(readOnly = true)
    public FAQResponse getFAQById(Long id) {
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FAQ not found with id: " + id));
        return mapToResponse(faq);
    }

    // Create FAQ
    @Transactional
    public FAQResponse createFAQ(FAQRequest request) {
        FAQ faq = new FAQ();
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setCategory(request.getCategory());
        faq.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        faq.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        
        FAQ saved = faqRepository.save(faq);
        return mapToResponse(saved);
    }

    // Update FAQ
    @Transactional
    public FAQResponse updateFAQ(Long id, FAQRequest request) {
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FAQ not found with id: " + id));
        
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setCategory(request.getCategory());
        faq.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : faq.getDisplayOrder());
        faq.setIsActive(request.getIsActive() != null ? request.getIsActive() : faq.getIsActive());
        
        FAQ updated = faqRepository.save(faq);
        return mapToResponse(updated);
    }

    // Delete FAQ
    @Transactional
    public void deleteFAQ(Long id) {
        if (!faqRepository.existsById(id)) {
            throw new EntityNotFoundException("FAQ not found with id: " + id);
        }
        faqRepository.deleteById(id);
    }

    // Toggle FAQ active status
    @Transactional
    public FAQResponse toggleFAQStatus(Long id) {
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FAQ not found with id: " + id));
        
        faq.setIsActive(!faq.getIsActive());
        FAQ updated = faqRepository.save(faq);
        return mapToResponse(updated);
    }

    // Map entity to response DTO
    private FAQResponse mapToResponse(FAQ faq) {
        FAQResponse response = new FAQResponse();
        response.setId(faq.getId());
        response.setQuestion(faq.getQuestion());
        response.setAnswer(faq.getAnswer());
        response.setCategory(faq.getCategory());
        response.setDisplayOrder(faq.getDisplayOrder());
        response.setIsActive(faq.getIsActive());
        response.setCreatedAt(faq.getCreatedAt());
        response.setUpdatedAt(faq.getUpdatedAt());
        return response;
    }
}

