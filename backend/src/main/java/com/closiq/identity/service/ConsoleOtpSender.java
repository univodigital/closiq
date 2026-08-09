package com.closiq.identity.service;

import com.closiq.config.ClosiqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleOtpSender implements OtpSender {

    private final ClosiqProperties properties;

    @Override
    public void sendOtp(String phone, String otp, String purpose) {
        if (properties.getOtp().isConsoleLogEnabled()) {
            log.info("OTP for {} ({}): {}", maskPhone(phone), purpose, otp);
        } else {
            log.debug("OTP dispatched to {} for purpose {}", maskPhone(phone), purpose);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
