package com.kuru.delivery.faq.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuru.delivery.common.dto.SuccessResponse;
import com.kuru.delivery.common.util.RoleValidationUtil;
import com.kuru.delivery.faq.dto.FAQRequest;
import com.kuru.delivery.faq.dto.FAQResponse;
import com.kuru.delivery.faq.service.FAQService;
import com.kuru.delivery.user.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/faqs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminFAQController {

	@Autowired
    private final FAQService faqService;

    @Autowired
    private UserRepository userRepository;

    public AdminFAQController(FAQService faqService) {
		this.faqService = faqService;
	}

	// Get all FAQs (including inactive)
    @GetMapping
    public ResponseEntity<List<FAQResponse>> getAllFAQs() {
        RoleValidationUtil.validateAdminRole(userRepository);
        List<FAQResponse> faqs = faqService.getAllFAQs();
        return ResponseEntity.ok(faqs);
    }

    // Get FAQ by ID
    @GetMapping("/{id}")
    public ResponseEntity<FAQResponse> getFAQById(@PathVariable Long id) {
        RoleValidationUtil.validateAdminRole(userRepository);
        FAQResponse faq = faqService.getFAQById(id);
        return ResponseEntity.ok(faq);
    }

    // Create FAQ
    @PostMapping
    public ResponseEntity<FAQResponse> createFAQ(@Valid @RequestBody FAQRequest request) {
        RoleValidationUtil.validateAdminRole(userRepository);
        FAQResponse faq = faqService.createFAQ(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(faq);
    }

    // Update FAQ
    @PutMapping("/{id}")
    public ResponseEntity<FAQResponse> updateFAQ(
            @PathVariable Long id,
            @Valid @RequestBody FAQRequest request) {
        RoleValidationUtil.validateAdminRole(userRepository);
        FAQResponse faq = faqService.updateFAQ(id, request);
        return ResponseEntity.ok(faq);
    }

    // Delete FAQ
    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse> deleteFAQ(@PathVariable Long id) {
        RoleValidationUtil.validateAdminRole(userRepository);
        faqService.deleteFAQ(id);
        return ResponseEntity.ok(new SuccessResponse("FAQ deleted successfully"));
    }

    // Toggle FAQ active status
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<FAQResponse> toggleFAQStatus(@PathVariable Long id) {
        RoleValidationUtil.validateAdminRole(userRepository);
        FAQResponse faq = faqService.toggleFAQStatus(id);
        return ResponseEntity.ok(faq);
    }
}

