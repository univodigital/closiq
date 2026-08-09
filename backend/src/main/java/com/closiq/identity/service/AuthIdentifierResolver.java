package com.closiq.identity.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;

import java.util.regex.Pattern;

public final class AuthIdentifierResolver {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern INDIAN_MOBILE = Pattern.compile("^[6-9]\\d{9}$");

    public enum Type {
        PHONE,
        EMAIL
    }

    public record ResolvedIdentifier(Type type, String phone, String email) {
    }

    private AuthIdentifierResolver() {
    }

    public static ResolvedIdentifier resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Phone or email is required");
        }

        String trimmed = raw.trim();
        if (trimmed.contains("@")) {
            String email = trimmed.toLowerCase();
            if (!EMAIL.matcher(email).matches()) {
                throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Enter a valid email address");
            }
            return new ResolvedIdentifier(Type.EMAIL, null, email);
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        }
        if (!INDIAN_MOBILE.matcher(digits).matches()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Enter a valid 10-digit mobile number or email");
        }
        return new ResolvedIdentifier(Type.PHONE, "+91" + digits, null);
    }
}
