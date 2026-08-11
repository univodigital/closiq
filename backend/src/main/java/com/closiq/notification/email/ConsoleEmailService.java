package com.closiq.notification.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "closiq.mail.enabled", havingValue = "false", matchIfMissing = true)
public class ConsoleEmailService implements EmailService {

    @Override
    public void sendOtp(String toEmail, String otp, String purpose) {
        log.info("EMAIL OTP to {} ({}): {}", maskEmail(toEmail), purpose, otp);
    }

    @Override
    public void sendWelcome(String toEmail, String displayName) {
        log.info("Welcome email to {} for {}", maskEmail(toEmail), displayName);
    }

    @Override
    public void sendPasswordResetOtp(String toEmail, String otp) {
        log.info("Password reset OTP email to {}: {}", maskEmail(toEmail), otp);
    }

    @Override
    public void sendOrderConfirmed(String toEmail, TransactionalEmailContext context) {
        log.info("Order confirmed email to {} for {}", maskEmail(toEmail), context.orderLabel());
    }

    @Override
    public void sendOutForDelivery(String toEmail, TransactionalEmailContext context) {
        log.info("Out for delivery email to {} for {}", maskEmail(toEmail), context.orderLabel());
    }

    @Override
    public void sendReturnReminder(String toEmail, TransactionalEmailContext context) {
        log.info("Return reminder email to {} for {}", maskEmail(toEmail), context.orderLabel());
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
