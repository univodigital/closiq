package com.closiq.payment.gateway;

import com.closiq.common.util.IdGenerator;
import com.closiq.config.ClosiqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class StubRazorpayPaymentGateway implements PaymentGateway {

    private final ClosiqProperties properties;

    @Override
    public RazorpayOrderResult createOrder(long amountPaise, String currency, String receiptId) {
        String orderId = "order_STUB_" + IdGenerator.uuidV7().toString().replace("-", "").substring(0, 14);
        log.debug("Stub Razorpay order created: {} amount={} receipt={}", orderId, amountPaise, receiptId);
        return RazorpayOrderResult.builder()
                .providerOrderId(orderId)
                .amountPaise(amountPaise)
                .currency(currency)
                .build();
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        String expected = RazorpayHmac.sign(
                orderId + "|" + paymentId,
                properties.getRazorpay().getKeySecret());
        return expected.equalsIgnoreCase(signature);
    }

    @Override
    public RazorpayRefundResult createRefund(String providerPaymentId, long amountPaise, String idempotencyKey) {
        if (amountPaise < 100) {
            throw new IllegalArgumentException("Refund amount must be at least 100 paise");
        }
        String refundId = "rfnd_STUB_" + IdGenerator.uuidV7().toString().replace("-", "").substring(0, 14);
        log.debug("Stub Razorpay refund created: {} payment={} amount={}", refundId, providerPaymentId, amountPaise);
        return RazorpayRefundResult.builder()
                .providerRefundId(refundId)
                .amountPaise(amountPaise)
                .status("processed")
                .build();
    }

    /** Dev helper: generate a valid stub signature for testing. */
    public String generateStubSignature(String orderId, String paymentId) {
        return RazorpayHmac.sign(
                orderId + "|" + paymentId,
                properties.getRazorpay().getKeySecret());
    }
}
