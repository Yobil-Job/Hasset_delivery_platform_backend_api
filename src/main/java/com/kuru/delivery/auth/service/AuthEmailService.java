package com.kuru.delivery.auth.service;

import com.kuru.delivery.auth.dto.ForgotPasswordRequest;
import com.kuru.delivery.auth.dto.ResetPasswordRequest;
import com.kuru.delivery.auth.dto.VerifyEmailRequest;
import com.kuru.delivery.common.dto.SuccessResponse;
import com.kuru.delivery.common.exception.AlreadyVerifiedException;
import com.kuru.delivery.common.exception.EmailNotFoundException;
import com.kuru.delivery.common.exception.InvalidCodeException;
import com.kuru.delivery.common.exception.WeakPasswordException;
import com.kuru.delivery.common.util.PasswordValidator;
import com.kuru.delivery.common.util.VerificationCodeGenerator;
import com.kuru.delivery.email.EmailService;
import com.kuru.delivery.user.model.User;
import com.kuru.delivery.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthEmailService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthEmailService(UserRepository userRepository,
                            EmailService emailService,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Generates and sends an email verification code for the given user.
     * Code expires in 10 minutes.
     */
    @Transactional
    public void sendVerificationCode(User user) {
        String code = VerificationCodeGenerator.generate6DigitCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10)); // Expires in 10 minutes
        user.setEmailVerified(true);
        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), code);
    }

    @Transactional
    public SuccessResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EmailNotFoundException(request.getEmail()));

        if (user.isEmailVerified()) {
            throw new AlreadyVerifiedException(request.getEmail());
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(request.getCode())) {
            throw new InvalidCodeException("Invalid verification code");
        }

        // Check if verification code has expired
        if (user.getVerificationCodeExpiry() != null && 
            user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidCodeException("Verification code has expired. Please request a new one.");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);

        return new SuccessResponse("Email verified successfully");
    }

    @Transactional
    public SuccessResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EmailNotFoundException(request.getEmail()));

        // Always generate a fresh code for security
        String code = VerificationCodeGenerator.generate6DigitCode();
        user.setPasswordResetCode(code);
        user.setPasswordResetCodeExpiry(LocalDateTime.now().plusMinutes(5)); // Expires in 5 minutes
        userRepository.save(user);

        // Send email with the code (email service ensures only one code is in the email body)
        emailService.sendPasswordResetEmail(user.getEmail(), code);
        return new SuccessResponse("Password reset code sent to email");
    }

    @Transactional
    public SuccessResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EmailNotFoundException(request.getEmail()));

        if (user.getPasswordResetCode() == null || !user.getPasswordResetCode().equals(request.getCode())) {
            throw new InvalidCodeException("Invalid password reset code");
        }

        // Check if password reset code has expired
        if (user.getPasswordResetCodeExpiry() != null && 
            user.getPasswordResetCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidCodeException("Password reset code has expired. Please request a new one.");
        }

        // Validate new password strength
        String passwordError = PasswordValidator.validateWithMessage(request.getNewPassword());
        if (passwordError != null) {
            throw new WeakPasswordException(passwordError);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetCode(null);
        user.setPasswordResetCodeExpiry(null);
        userRepository.save(user);

        return new SuccessResponse("Password reset successfully");
    }

    /**
     * Resends verification code to user's email.
     */
    @Transactional
    public SuccessResponse resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException(email));

        if (user.isEmailVerified()) {
            throw new AlreadyVerifiedException(email);
        }

        sendVerificationCode(user);
        return new SuccessResponse("Verification code resent to your email");
    }
}


