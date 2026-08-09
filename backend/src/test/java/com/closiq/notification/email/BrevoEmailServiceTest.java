package com.closiq.notification.email;

import com.closiq.config.BrevoProperties;
import com.closiq.config.ClosiqProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class BrevoEmailServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpServer httpServer;
    private URI sendUri;
    private final AtomicReference<String> capturedApiKey = new AtomicReference<>();
    private final AtomicReference<String> capturedBody = new AtomicReference<>();

    private BrevoProperties brevoProperties;
    private ClosiqProperties closiqProperties;
    private BrevoEmailService emailService;

    @BeforeEach
    void setUp() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/v3/smtp/email", this::handleSend);
        httpServer.start();
        sendUri = URI.create("http://127.0.0.1:" + httpServer.getAddress().getPort() + "/v3/smtp/email");

        brevoProperties = new BrevoProperties();
        brevoProperties.setApiKey("test-brevo-api-key");
        brevoProperties.setSenderEmail("noreply@closiq.com");
        brevoProperties.setSenderName("Closiq");

        closiqProperties = new ClosiqProperties();

        emailService = new BrevoEmailService(
                brevoProperties,
                closiqProperties,
                OBJECT_MAPPER,
                HttpClient.newHttpClient(),
                sendUri);
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void sendOtp_postsCorrectPayloadAndApiKeyHeader() throws Exception {
        emailService.sendOtp("user@example.com", "123456", "LOGIN");

        assertThat(capturedApiKey.get()).isEqualTo("test-brevo-api-key");

        JsonNode body = OBJECT_MAPPER.readTree(capturedBody.get());
        assertThat(body.path("sender").path("email").asText()).isEqualTo("noreply@closiq.com");
        assertThat(body.path("sender").path("name").asText()).isEqualTo("Closiq");
        assertThat(body.path("to").get(0).path("email").asText()).isEqualTo("user@example.com");
        assertThat(body.path("subject").asText()).isEqualTo("Your Closiq login code");
        assertThat(body.path("htmlContent").asText()).contains("123456");
    }

    @Test
    void sendWelcome_mapsRecipientSubjectAndContent() throws Exception {
        emailService.sendWelcome("newuser@example.com", "Alex");

        JsonNode body = OBJECT_MAPPER.readTree(capturedBody.get());
        assertThat(body.path("to").get(0).path("email").asText()).isEqualTo("newuser@example.com");
        assertThat(body.path("subject").asText()).isEqualTo("Welcome to Closiq");
        assertThat(body.path("htmlContent").asText()).contains("Alex");
    }

    @Test
    void sendHtml_handlesBrevoApiFailure() {
        capturedBody.set(null);
        httpServer.removeContext("/v3/smtp/email");
        httpServer.createContext("/v3/smtp/email", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 500, "error");
        });

        emailService.sendOtp("user@example.com", "654321", "REGISTER");

        assertThat(capturedBody.get()).contains("654321");
    }

    @Test
    void sendHtml_doesNotSendWhenApiKeyMissing() {
        brevoProperties.setApiKey("");
        emailService = new BrevoEmailService(
                brevoProperties, closiqProperties, OBJECT_MAPPER, HttpClient.newHttpClient(), sendUri);

        emailService.sendOtp("user@example.com", "111111", "REGISTER");

        assertThat(capturedBody.get()).isNull();
    }

    @Test
    void sendHtml_fallsBackToClosiqMailSenderWhenBrevoSenderMissing() throws Exception {
        brevoProperties.setSenderEmail("");
        brevoProperties.setSenderName("");
        closiqProperties.getMail().setFrom("fallback@closiq.com");
        closiqProperties.getMail().setFromName("Closiq Fallback");

        emailService = new BrevoEmailService(
                brevoProperties, closiqProperties, OBJECT_MAPPER, HttpClient.newHttpClient(), sendUri);
        emailService.sendOtp("user@example.com", "222222", "RESET");

        JsonNode body = OBJECT_MAPPER.readTree(capturedBody.get());
        assertThat(body.path("sender").path("email").asText()).isEqualTo("fallback@closiq.com");
        assertThat(body.path("sender").path("name").asText()).isEqualTo("Closiq Fallback");
        assertThat(body.path("subject").asText()).isEqualTo("Your Closiq password reset code");
    }

    private void handleSend(HttpExchange exchange) throws IOException {
        capturedApiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
        capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        respond(exchange, 201, "{\"messageId\":\"test-message-id\"}");
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
