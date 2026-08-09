package com.closiq.notification.email;

import com.closiq.config.ClosiqProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "closiq.mail.enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final ClosiqProperties properties;

    @Override
    public void sendOtp(String toEmail, String otp, String purpose) {
        String subject = switch (purpose) {
            case "REGISTER" -> "Your Closiq verification code";
            case "LOGIN" -> "Your Closiq login code";
            case "RESET" -> "Your Closiq password reset code";
            default -> "Your Closiq verification code";
        };
        sendHtml(
                toEmail,
                subject,
                """
                <p>Your Closiq verification code is:</p>
                <p style="font-size:24px;font-weight:bold;letter-spacing:4px;">%s</p>
                <p>This code expires in 5 minutes. Do not share it with anyone.</p>
                """.formatted(otp));
    }

    @Override
    public void sendWelcome(String toEmail, String displayName) {
        sendHtml(
                toEmail,
                "Welcome to Closiq",
                """
                <p>Hi %s,</p>
                <p>Your Closiq account is ready. Start exploring designer rentals for your next occasion.</p>
                """.formatted(displayName));
    }

    @Override
    public void sendPasswordResetOtp(String toEmail, String otp) {
        sendOtp(toEmail, otp, "RESET");
    }

    private void sendHtml(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.getMail().getFrom(), properties.getMail().getFromName());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.debug("Email sent to {}", toEmail);
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("Failed to send email to {}", toEmail, ex);
            throw new IllegalStateException("Failed to send email", ex);
        }
    }
}
