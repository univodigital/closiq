package com.closiq.identity.service;

import com.closiq.notification.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompositeOtpSender implements OtpSender {

    private final EmailService emailService;

    @Override
    public void sendOtp(String phone, String email, String otp, String purpose) {
        log.info("OTP for {} ({}): {} — SMS delivery not configured; use this code to verify", maskPhone(phone), purpose, otp);

        if (email != null && !email.isBlank()) {
            try {
                emailService.sendOtp(email, otp, purpose);
            } catch (Exception ex) {
                log.warn("Email OTP delivery failed for {} ({}): {}", maskEmail(email), purpose, ex.getMessage());
            }
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "****" + email.substring(at);
        }
        return email.charAt(0) + "****" + email.substring(at);
    }
}
