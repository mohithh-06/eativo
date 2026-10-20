package com.example.demo.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.dto.OtpRequest;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

@Service
public class OtpService {

    @Value("${app.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, PendingRegistration> otpCache = new ConcurrentHashMap<>();

    public static class PendingRegistration {
        private final OtpRequest otpRequest;
        private final String otpCode;
        private final LocalDateTime expiresAt;

        public PendingRegistration(OtpRequest otpRequest, String otpCode, LocalDateTime expiresAt) {
            this.otpRequest = otpRequest;
            this.otpCode = otpCode;
            this.expiresAt = expiresAt;
        }

        public OtpRequest getOtpRequest() {
            return otpRequest;
        }

        public String getOtpCode() {
            return otpCode;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
    }

    public String generateAndStoreOtp(OtpRequest request) {
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        PendingRegistration pending = new PendingRegistration(
                request,
                otpCode,
                LocalDateTime.now().plusMinutes(5)
        );
        otpCache.put(request.getEmail().toLowerCase().trim(), pending);
        return otpCode;
    }

    public void sendOtpEmail(String email, String otpCode) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException("Email delivery is not configured. Set RESEND_API_KEY in the backend environment.");
        }

        String subject = "Your Eativo Verification Code";
        String text = "Welcome to Eativo!\n\n"
                + "Your 6-digit verification code is: " + otpCode + "\n\n"
                + "This code is valid for 5 minutes. If you did not request this code, please ignore this email.\n\n"
                + "Happy Dining,\n"
                + "The Eativo Team";

        try {
            Resend resend = new Resend(resendApiKey);
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(email)
                    .subject(subject)
                    .text(text)
                    .build();

            CreateEmailResponse response = resend.emails().send(options);
            System.out.println("Eativo verification email accepted by Resend. Email ID: " + response.getId());
        } catch (ResendException | RuntimeException e) {
            System.err.println("Failed to send Eativo verification email through Resend: " + e.getMessage());
            throw new IllegalStateException("Could not send verification email. Please try again later.", e);
        }
    }

    public PendingRegistration getPendingRegistration(String email) {
        if (email == null) {
            return null;
        }
        return otpCache.get(email.toLowerCase().trim());
    }

    public void removePendingRegistration(String email) {
        if (email != null) {
            otpCache.remove(email.toLowerCase().trim());
        }
    }
}
