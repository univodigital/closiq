package com.closiq.payment.gateway;

public interface PaymentGateway {

    RazorpayOrderResult createOrder(long amountPaise, String currency, String receiptId);

    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);

    /**
     * Refund to the original payment method. Amount in paise.
     * Idempotency key is forwarded to the provider when supported.
     */
    RazorpayRefundResult createRefund(String providerPaymentId, long amountPaise, String idempotencyKey);
}
