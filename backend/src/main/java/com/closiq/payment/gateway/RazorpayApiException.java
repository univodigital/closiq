package com.closiq.payment.gateway;

import lombok.Getter;

@Getter
public class RazorpayApiException extends RuntimeException {

    private final int statusCode;

    public RazorpayApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
