package com.kuru.delivery.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.admin}")
    private String adminAddress;

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${RESEND_FROM_EMAIL:}")
    private String resendFromEmail;

    @Value("${RESEND_FROM_NAME:Haset Delivery}")
    private String resendFromName;

    private final WebClient resendClient = WebClient.builder()
            .baseUrl("https://api.resend.com")
            .build();

    public void sendVerificationEmail(String to, String code) {
        String subject = "Verify Your Email - ሀሴት Delivery";
        String htmlBody = buildVerificationEmailHtml(code);
        sendHtmlMessage(to, subject, htmlBody);
    }

    public void sendPasswordResetEmail(String to, String code) {
        String subject = "Password Reset Code - ሀሴት Delivery";
        String htmlBody = buildPasswordResetEmailHtml(code);
        sendHtmlMessage(to, subject, htmlBody);
    }

    public void sendContactEmail(String fromEmail, String subject, String messageBody) {
        String subjectLine = "[Contact] " + subject;
        String text = "From: " + fromEmail + System.lineSeparator() + System.lineSeparator() + messageBody;
        sendSimpleMessage(adminAddress, subjectLine, text);
    }

    private void sendHtmlMessage(String to, String subject, String htmlBody) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            logger.error("RESEND_API_KEY is not configured. Cannot send email to {}", to);
            throw new com.kuru.delivery.common.exception.EmailSendingException(
                "Email service is not configured. Please contact support.", null);
        }

        String from = String.format("%s <%s>", resendFromName, resendFromEmail != null && !resendFromEmail.isBlank() ? resendFromEmail : fromAddress);

        try {
            resendClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + resendApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                    "from", from,
                    "to", List.of(to),
                    "subject", subject,
                    "html", htmlBody
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .block();

            logger.info("Email sent successfully to: {} via Resend", to);
        } catch (Exception e) {
            logger.error("Unexpected error sending email via Resend to: {}", to, e);
            String userMessage = "Unable to send email at this time. Please try again later or contact support.";
            throw new com.kuru.delivery.common.exception.EmailSendingException(userMessage, e);
        }
    }

    private void sendSimpleMessage(String to, String subject, String text) {
        // For admin contact email, we can also send plain-text using the same Resend API
        if (resendApiKey == null || resendApiKey.isBlank()) {
            logger.error("RESEND_API_KEY is not configured. Cannot send simple email to {}", to);
            throw new com.kuru.delivery.common.exception.EmailSendingException(
                "Email service is not configured. Please contact support.", null);
        }

        String from = String.format("%s <%s>", resendFromName, resendFromEmail != null && !resendFromEmail.isBlank() ? resendFromEmail : fromAddress);

        try {
            resendClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + resendApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                    "from", from,
                    "to", List.of(to),
                    "subject", subject,
                    "text", text
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .block();

            logger.info("Simple email sent successfully to: {} via Resend", to);
        } catch (Exception e) {
            logger.error("Failed to send simple email via Resend to: {}", to, e);
            throw new com.kuru.delivery.common.exception.EmailSendingException(
                "Unable to send email at this time. Please try again later or contact support.", e);
        }
    }

    private String buildVerificationEmailHtml(String code) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #f97316 0%, #ea580c 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .code-box { background: white; border: 2px dashed #f97316; border-radius: 8px; padding: 20px; text-align: center; margin: 25px 0; }
                    .code { font-size: 32px; font-weight: bold; color: #f97316; letter-spacing: 8px; font-family: 'Courier New', monospace; }
                    .info { background: #e0f2fe; border-left: 4px solid #0ea5e9; padding: 15px; margin: 20px 0; border-radius: 4px; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #e5e7eb; color: #6b7280; font-size: 14px; }
                    .button { display: inline-block; background: #f97316; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📦 ሀሴት Delivery</h1>
                        <p style="margin: 10px 0 0 0; opacity: 0.9;">Your Trusted Delivery Partner in Ethiopia</p>
                    </div>
                    <div class="content">
                        <h2 style="color: #1f2937; margin-top: 0;">Welcome to ሀሴት Delivery!</h2>
                        <p>Thank you for joining ሀሴት Delivery, Ethiopia's premier delivery platform. We're excited to have you on board!</p>
                        
                        <p>To complete your registration and start using our services, please verify your email address using the verification code below:</p>
                        
                        <div class="code-box">
                            <p style="margin: 0 0 10px 0; color: #6b7280; font-size: 14px;">Your Verification Code</p>
                            <div class="code">{VERIFICATION_CODE}</div>
                            <p style="margin: 10px 0 0 0; color: #6b7280; font-size: 12px;">This code will expire in 10 minutes</p>
                        </div>
                        
                        <div class="info">
                            <strong>💡 About ሀሴት Delivery:</strong>
                            <ul style="margin: 10px 0; padding-left: 20px;">
                                <li>Fast and reliable delivery services across Ethiopia</li>
                                <li>Real-time package tracking</li>
                                <li>Competitive pricing with no hidden fees</li>
                                <li>24/7 customer support</li>
                                <li>Secure and safe delivery handling</li>
                            </ul>
                        </div>
                        
                        <p style="margin-top: 25px;">If you didn't create an account with ሀሴት Delivery, please ignore this email.</p>
                        
                        <p style="margin-top: 20px;">Best regards,<br><strong>The ሀሴት Delivery Team</strong></p>
                    </div>
                    <div class="footer">
                        <p>© 2024 ሀሴት Delivery. All rights reserved.</p>
                        <p>Bole Road, Addis Ababa, Ethiopia | support@hasetdelivery.et</p>
                    </div>
                </div>
            </body>
            </html>
            """;
        return template.replace("{VERIFICATION_CODE}", code);
    }

    private String buildPasswordResetEmailHtml(String code) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #f97316 0%, #ea580c 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .code-box { background: white; border: 2px dashed #f97316; border-radius: 8px; padding: 20px; text-align: center; margin: 25px 0; }
                    .code { font-size: 32px; font-weight: bold; color: #f97316; letter-spacing: 8px; font-family: 'Courier New', monospace; }
                    .warning { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; border-radius: 4px; }
                    .info { background: #e0f2fe; border-left: 4px solid #0ea5e9; padding: 15px; margin: 20px 0; border-radius: 4px; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #e5e7eb; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 ሀሴት Delivery</h1>
                        <p style="margin: 10px 0 0 0; opacity: 0.9;">Password Reset Request</p>
                    </div>
                    <div class="content">
                        <h2 style="color: #1f2937; margin-top: 0;">Reset Your Password</h2>
                        <p>We received a request to reset your password for your ሀሴት Delivery account.</p>
                        
                        <p>Use the code below to reset your password:</p>
                        
                        <div class="code-box">
                            <p style="margin: 0 0 10px 0; color: #6b7280; font-size: 14px;">Your Password Reset Code</p>
                            <div class="code">{RESET_CODE}</div>
                            <p style="margin: 10px 0 0 0; color: #6b7280; font-size: 12px;">This code will expire in 10 minutes</p>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ Security Notice:</strong>
                            <p style="margin: 10px 0 0 0;">If you didn't request a password reset, please ignore this email. Your account remains secure.</p>
                        </div>
                        
                        <div class="info">
                            <strong>💡 About ሀሴት Delivery:</strong>
                            <p style="margin: 10px 0 0 0;">ሀሴት Delivery is Ethiopia's trusted delivery platform, providing fast, reliable, and secure delivery services across the country. We're committed to making your delivery experience seamless and convenient.</p>
                            <ul style="margin: 10px 0; padding-left: 20px;">
                                <li>Real-time package tracking</li>
                                <li>Secure delivery handling</li>
                                <li>24/7 customer support</li>
                                <li>Competitive pricing</li>
                            </ul>
                        </div>
                        
                        <p style="margin-top: 25px;">Need help? Contact our support team at support@hasetdelivery.et</p>
                        
                        <p style="margin-top: 20px;">Best regards,<br><strong>The ሀሴት Delivery Team</strong></p>
                    </div>
                    <div class="footer">
                        <p>© 2024 ሀሴት Delivery. All rights reserved.</p>
                        <p>Bole Road, Addis Ababa, Ethiopia | support@hasetdelivery.et</p>
                    </div>
                </div>
            </body>
            </html>
            """;
        return template.replace("{RESET_CODE}", code);
    }
}


