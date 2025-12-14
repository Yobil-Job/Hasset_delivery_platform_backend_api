package com.kuru.delivery.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kuru.delivery.auth.dto.ForgotPasswordRequest;
import com.kuru.delivery.auth.dto.LoginRequest;
import com.kuru.delivery.auth.dto.LoginResponse;
import com.kuru.delivery.auth.dto.RefreshTokenRequest;
import com.kuru.delivery.auth.dto.ResetPasswordRequest;
import com.kuru.delivery.auth.dto.VerifyEmailRequest;
import com.kuru.delivery.auth.dto.RegisterRequest;
import com.kuru.delivery.auth.model.RefreshToken;
import com.kuru.delivery.auth.service.RefreshTokenService;
import com.kuru.delivery.auth.service.AuthEmailService;
import com.kuru.delivery.auth.service.LoginAttemptService;
import com.kuru.delivery.auth.service.TokenBlacklistService;
import com.kuru.delivery.common.dto.SuccessResponse;
import com.kuru.delivery.common.exception.AccountLockedException;
import com.kuru.delivery.common.exception.WeakPasswordException;
import com.kuru.delivery.common.util.PasswordValidator;
import com.kuru.delivery.config.JwtUtil;
import com.kuru.delivery.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kuru.delivery.user.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuthEmailService authEmailService;
    private final LoginAttemptService loginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;
    
    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository,
            JwtUtil jwtUtil, RefreshTokenService refreshTokenService, AuthEmailService authEmailService,
            LoginAttemptService loginAttemptService, TokenBlacklistService tokenBlacklistService) {
        super();
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.authEmailService = authEmailService;
        this.loginAttemptService = loginAttemptService;
        this.tokenBlacklistService = tokenBlacklistService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Check if account is locked
            if (loginAttemptService.isAccountLocked(request.getEmail())) {
                long remainingSeconds = loginAttemptService.getLockoutRemainingSeconds(request.getEmail());
                long remainingMinutes = (remainingSeconds + 59) / 60; // Round up to minutes
                throw new AccountLockedException(
                    "Account is locked due to too many failed login attempts. Please try again in " + 
                    remainingMinutes + " minute(s).", 
                    remainingSeconds
                );
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if email is verified (except for ADMIN role)
            if (!user.isEmailVerified() && user.getRole() != com.kuru.delivery.user.model.Role.ADMIN) {
                throw new com.kuru.delivery.common.exception.EmailNotVerifiedException(user.getEmail());
            }

            // Record successful login and clear failed attempts
            loginAttemptService.recordSuccessfulLogin(request.getEmail());

            String accessToken = jwtUtil.generateAccessToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );

            refreshTokenService.deleteByUserId(user.getId());
            
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
     
            return ResponseEntity.ok(
                    new LoginResponse(accessToken, refreshToken.getToken(), user.getRole().name())
            );
        } catch (AccountLockedException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.LOCKED)
                    .body(java.util.Map.of(
                        "error", "Account locked", 
                        "message", e.getMessage(),
                        "lockoutRemainingSeconds", e.getLockoutRemainingSeconds()
                    ));
        } catch (com.kuru.delivery.common.exception.EmailNotVerifiedException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "Email not verified", "message", e.getMessage()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            // Record failed login attempt
            loginAttemptService.recordFailedAttempt(request.getEmail());
            int remainingAttempts = loginAttemptService.getRemainingAttempts(request.getEmail());
            
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of(
                        "error", "Invalid credentials", 
                        "message", "Email or password is incorrect",
                        "remainingAttempts", remainingAttempts
                    ));
        } catch (Exception e) {
            // Don't expose internal error details to users
            String userMessage = "Login failed. Please try again later.";
            if (e.getMessage() != null && e.getMessage().contains("email")) {
                userMessage = "Unable to process login. Please try again or contact support.";
            }
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Login failed", "message", userMessage));
        }
    }
 
     @PostMapping("/refresh")
        public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
            String refreshTokenStr = request.getRefreshToken();
            
            RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            refreshTokenService.verifyExpiration(refreshToken);

            User user = refreshToken.getUser();

           
            String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

           
            refreshTokenService.deleteByUserId(user.getId());
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

            return ResponseEntity.ok(
                    new LoginResponse(newAccessToken, newRefreshToken.getToken(), user.getRole().name())
            );
        }

  
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@jakarta.validation.Valid @RequestBody RegisterRequest request) {
        try {
            // Validate password strength
            String passwordError = PasswordValidator.validateWithMessage(request.getPassword());
            if (passwordError != null) {
                throw new WeakPasswordException(passwordError);
            }

            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body(java.util.Map.of("error", "Email already exists", "message", "An account with this email already exists"));
            }

            User user = new User();
            user.setEmail(request.getEmail());
            // Use PasswordEncoder bean instead of creating new instance
            user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(request.getPassword()));
            user.setFirstname(request.getFirstname());
            user.setLastname(request.getLastname());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setAddress(request.getAddress());
            user.setRole(request.getRole() != null ? request.getRole() : com.kuru.delivery.user.model.Role.CUSTOMER);
            user.setAccountType(request.getAccountType());

            User saved = userRepository.save(user);

            // Generate and send verification code
            try {
                authEmailService.sendVerificationCode(saved);
                return ResponseEntity.ok(new SuccessResponse("User registered successfully. Verification code sent to email."));
            } catch (com.kuru.delivery.common.exception.EmailSendingException e) {
                // User is created but email failed - still return success but with warning
                // The user can request a new verification code later
                logger.warn("User registered but email sending failed: {}", e.getMessage());
                return ResponseEntity.status(org.springframework.http.HttpStatus.ACCEPTED)
                        .body(java.util.Map.of(
                            "message", "Account created successfully, but we couldn't send the verification email. Please try logging in to request a new verification code.",
                            "emailSent", false,
                            "email", saved.getEmail()
                        ));
            }
        } catch (WeakPasswordException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", "Weak password", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Registration failed", "message", "An unexpected error occurred. Please try again later."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request,
                                      jakarta.servlet.http.HttpServletRequest httpRequest) {
        String refreshTokenStr = request.getRefreshToken();

        refreshTokenService.findByToken(refreshTokenStr)
            .ifPresent(token -> {
                refreshTokenService.deleteByUserId(token.getUser().getId());
            });

        // Get access token from Authorization header if present
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7).trim();
            // Blacklist the access token
            long expirationSeconds = jwtUtil.getTokenExpirationSeconds(accessToken);
            if (expirationSeconds > 0) {
                tokenBlacklistService.blacklistToken(accessToken, expirationSeconds);
            }
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.GetMapping("/debug")
    public ResponseEntity<java.util.Map<String, Object>> debug(jakarta.servlet.http.HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        response.put("header", request.getHeader("Authorization"));
        if (auth != null) {
            response.put("name", auth.getName());
            response.put("authorities", auth.getAuthorities());
            response.put("isAuthenticated", auth.isAuthenticated());
            response.put("principal", auth.getPrincipal().toString());
        } else {
            response.put("auth", "null");
        }
        return ResponseEntity.ok(response);
    }
    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        return ResponseEntity.ok(user);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<SuccessResponse> verifyEmail(@RequestBody VerifyEmailRequest request) {
        SuccessResponse response = authEmailService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<SuccessResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        SuccessResponse response = authEmailService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<SuccessResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        SuccessResponse response = authEmailService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<SuccessResponse> resendVerification(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(new SuccessResponse("Email is required"));
        }
        SuccessResponse response = authEmailService.resendVerificationCode(email);
        return ResponseEntity.ok(response);
    }
}
