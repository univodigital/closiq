package com.closiq.payment.gateway;

import com.closiq.config.ClosiqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubRazorpayPaymentGatewayTest {

    private StubRazorpayPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        properties.getRazorpay().setKeySecret("test_secret");
        gateway = new StubRazorpayPaymentGateway(properties);
    }

    @Test
    void verifyPaymentSignature_acceptsValidHmac() {
        String orderId = "order_STUB_abc";
        String paymentId = "pay_STUB_xyz";
        String signature = gateway.generateStubSignature(orderId, paymentId);

        assertThat(gateway.verifyPaymentSignature(orderId, paymentId, signature)).isTrue();
    }

    @Test
    void verifyPaymentSignature_rejectsInvalidSignature() {
        assertThat(gateway.verifyPaymentSignature("order_1", "pay_1", "bad")).isFalse();
    }
}
