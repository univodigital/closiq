package com.closiq.payment.gateway;

public interface PaymentGateway {

    RazorpayOrderResult createOrder(long amountPaise, String currency, String receiptId);

    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);
}
