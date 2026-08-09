package com.closiq.identity.service;

public interface OtpSender {

    /**
     * Deliver OTP via SMS to phone and, when provided, via email.
     */
    void sendOtp(String phone, String email, String otp, String purpose);
}
