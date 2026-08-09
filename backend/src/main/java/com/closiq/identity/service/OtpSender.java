package com.closiq.identity.service;

public interface OtpSender {

    void sendOtp(String phone, String otp, String purpose);
}
