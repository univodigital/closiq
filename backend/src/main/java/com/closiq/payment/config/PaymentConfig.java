package com.closiq.payment.config;

import com.closiq.config.ClosiqProperties;
import com.closiq.payment.gateway.PaymentGateway;
import com.closiq.payment.gateway.RazorpayPaymentGateway;
import com.closiq.payment.gateway.StubRazorpayPaymentGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConfig {

    @Bean
    public PaymentGateway paymentGateway(
            ClosiqProperties properties,
            StubRazorpayPaymentGateway stubGateway,
            RazorpayPaymentGateway liveGateway) {
        return properties.getRazorpay().isStubEnabled() ? stubGateway : liveGateway;
    }
}
