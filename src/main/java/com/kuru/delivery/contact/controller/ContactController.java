package com.kuru.delivery.contact.controller;

import com.kuru.delivery.common.dto.SuccessResponse;
import com.kuru.delivery.contact.dto.ContactRequest;
import com.kuru.delivery.email.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final EmailService emailService;

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    public ResponseEntity<SuccessResponse> sendContact(@RequestBody ContactRequest request) {
        emailService.sendContactEmail(request.getEmail(), request.getSubject(), request.getMessage());
        return ResponseEntity.ok(new SuccessResponse("Message sent successfully"));
    }
}


