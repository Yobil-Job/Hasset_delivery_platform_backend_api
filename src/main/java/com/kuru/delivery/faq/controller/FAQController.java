package com.kuru.delivery.faq.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuru.delivery.faq.dto.FAQResponse;
import com.kuru.delivery.faq.service.FAQService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FAQController {

    private final FAQService faqService;
    
    // Public endpoint - Get all active FAQs
    @GetMapping
    public ResponseEntity<List<FAQResponse>> getAllActiveFAQs() {
        List<FAQResponse> faqs = faqService.getAllActiveFAQs();
        return ResponseEntity.ok(faqs);
    }
}

