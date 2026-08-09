package com.closiq.payment.gateway;

import com.closiq.config.ClosiqProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayPaymentGateway implements PaymentGateway {

    private static final URI ORDERS_URI = URI.create("https://api.razorpay.com/v1/orders");
    private static final long MIN_AMOUNT_PAISE = 100;
    private static final int RECEIPT_MAX_LENGTH = 40;

    private final ClosiqProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public RazorpayOrderResult createOrder(long amountPaise, String currency, String receiptId) {
        if (amountPaise < MIN_AMOUNT_PAISE) {
            throw new IllegalArgumentException("Amount must be at least 100 paise");
        }

        String receipt = receiptId == null ? "" : receiptId;
        if (receipt.length() > RECEIPT_MAX_LENGTH) {
            receipt = receipt.substring(0, RECEIPT_MAX_LENGTH);
        }

        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "amount", amountPaise,
                    "currency", currency == null || currency.isBlank() ? "INR" : currency,
                    "receipt", receipt));

            HttpRequest request = HttpRequest.newBuilder(ORDERS_URI)
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", basicAuthHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new RazorpayApiException(401, "Razorpay authentication failed");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Razorpay order creation failed: status={} body={}", response.statusCode(), response.body());
                throw new RazorpayApiException(response.statusCode(), "Razorpay order creation failed");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String orderId = json.path("id").asText(null);
            if (orderId == null || orderId.isBlank()) {
                throw new RazorpayApiException(500, "Razorpay response missing order id");
            }

            long amount = json.path("amount").asLong(amountPaise);
            String orderCurrency = json.path("currency").asText(currency);

            log.debug("Razorpay order created: {} amount={} receipt={}", orderId, amount, receipt);
            return RazorpayOrderResult.builder()
                    .providerOrderId(orderId)
                    .amountPaise(amount)
                    .currency(orderCurrency)
                    .build();
        } catch (RazorpayApiException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Razorpay order creation error", ex);
            throw new RazorpayApiException(500, "Unable to reach Razorpay");
        }
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        String expected = RazorpayHmac.sign(
                orderId + "|" + paymentId,
                properties.getRazorpay().getKeySecret());
        return expected.equalsIgnoreCase(signature);
    }

    private String basicAuthHeader() {
        String keyId = properties.getRazorpay().getKeyId();
        String keySecret = properties.getRazorpay().getKeySecret();
        String token = Base64.getEncoder().encodeToString(
                (keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }
}
