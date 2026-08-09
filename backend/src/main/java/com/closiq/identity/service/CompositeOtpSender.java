package com.closiq.identity.service;

import com.closiq.config.ClosiqProperties;
import com.closiq.notification.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompositeOtpSender implements OtpSender {

    private final ClosiqProperties properties;
    private final EmailService emailService;

    @Override
    public void sendOtp(String phone, String email, String otp, String purpose) {
        sendSms(phone, otp, purpose);
        if (email != null && !email.isBlank()) {
            emailService.sendOtp(email, otp, purpose);
        }
    }

    private void sendSms(String phone, String otp, String purpose) {
        if (properties.getOtp().isConsoleLogEnabled()) {
            log.info("SMS OTP for {} ({}): {}", maskPhone(phone), purpose, otp);
        } else {
            log.debug("SMS OTP dispatched to {} for purpose {}", maskPhone(phone), purpose);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
