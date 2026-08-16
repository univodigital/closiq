package com.closiq.payment.config;

import com.closiq.config.ClosiqProperties;
import com.closiq.payment.gateway.PaymentGateway;
import com.closiq.payment.gateway.RazorpayPaymentGateway;
import com.closiq.payment.gateway.StubRazorpayPaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PaymentConfig {

    @Bean
    public PaymentGateway paymentGateway(
            ClosiqProperties properties,
            StubRazorpayPaymentGateway stubGateway,
            RazorpayPaymentGateway liveGateway) {
        boolean useStub = properties.getRazorpay().shouldUseStubGateway();
        log.info("Payment gateway: {}", useStub ? "STUB (mock Razorpay)" : "LIVE Razorpay API");
        return useStub ? stubGateway : liveGateway;
    }
}
