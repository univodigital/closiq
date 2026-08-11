package com.closiq.notification.email;

import com.closiq.config.BrevoProperties;
import com.closiq.config.ClosiqProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "closiq.mail.enabled", havingValue = "true")
public class BrevoEmailService implements EmailService {

    static final URI DEFAULT_SEND_URI = URI.create("https://api.brevo.com/v3/smtp/email");

    private final BrevoProperties brevoProperties;
    private final ClosiqProperties closiqProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI sendUri;

    @Autowired
    public BrevoEmailService(
            BrevoProperties brevoProperties,
            ClosiqProperties closiqProperties,
            ObjectMapper objectMapper) {
        this(
                brevoProperties,
                closiqProperties,
                objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                DEFAULT_SEND_URI);
    }

    static BrevoEmailService forTesting(
            BrevoProperties brevoProperties,
            ClosiqProperties closiqProperties,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            URI sendUri) {
        return new BrevoEmailService(brevoProperties, closiqProperties, objectMapper, httpClient, sendUri);
    }

    private BrevoEmailService(
            BrevoProperties brevoProperties,
            ClosiqProperties closiqProperties,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            URI sendUri) {
        this.brevoProperties = brevoProperties;
        this.closiqProperties = closiqProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.sendUri = sendUri;
    }

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

    @Override
    public void sendOrderConfirmed(String toEmail, TransactionalEmailContext context) {
        sendHtml(
                toEmail,
                "Order confirmed — " + context.orderLabel(),
                TransactionalEmailTemplates.orderConfirmed(context));
    }

    @Override
    public void sendOutForDelivery(String toEmail, TransactionalEmailContext context) {
        sendHtml(
                toEmail,
                "Your Closiq order is out for delivery",
                TransactionalEmailTemplates.outForDelivery(context));
    }

    @Override
    public void sendReturnReminder(String toEmail, TransactionalEmailContext context) {
        sendHtml(
                toEmail,
                "Your Closiq rental ends soon",
                TransactionalEmailTemplates.returnReminder(context));
    }

    private void sendHtml(String toEmail, String subject, String htmlBody) {
        if (brevoProperties.getApiKey() == null || brevoProperties.getApiKey().isBlank()) {
            log.warn("Failed to send email to {}: Brevo API key not configured", maskEmail(toEmail));
            return;
        }

        String senderEmail = resolveSenderEmail();
        if (senderEmail == null || senderEmail.isBlank()) {
            log.warn("Failed to send email to {}: sender email not configured", maskEmail(toEmail));
            return;
        }

        try {
            Map<String, Object> body = Map.of(
                    "sender",
                    Map.of("email", senderEmail, "name", resolveSenderName()),
                    "to",
                    List.of(Map.of("email", toEmail)),
                    "subject",
                    subject,
                    "htmlContent",
                    htmlBody);

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder(sendUri)
                    .timeout(Duration.ofSeconds(5))
                    .header("api-key", brevoProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn(
                        "Failed to send email to {}: Brevo API returned status {}",
                        maskEmail(toEmail),
                        response.statusCode());
                return;
            }

            log.info("Email sent to {}", maskEmail(toEmail));
        } catch (Exception ex) {
            log.warn("Failed to send email to {}: {}", maskEmail(toEmail), ex.getMessage());
        }
    }

    private String resolveSenderEmail() {
        if (brevoProperties.getSenderEmail() != null && !brevoProperties.getSenderEmail().isBlank()) {
            return brevoProperties.getSenderEmail();
        }
        return closiqProperties.getMail().getFrom();
    }

    private String resolveSenderName() {
        if (brevoProperties.getSenderName() != null && !brevoProperties.getSenderName().isBlank()) {
            return brevoProperties.getSenderName();
        }
        return closiqProperties.getMail().getFromName();
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
