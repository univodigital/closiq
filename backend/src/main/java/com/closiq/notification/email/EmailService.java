package com.closiq.notification.email;

public interface EmailService {

    void sendOtp(String toEmail, String otp, String purpose);

    void sendWelcome(String toEmail, String displayName);

    void sendPasswordResetOtp(String toEmail, String otp);
}
