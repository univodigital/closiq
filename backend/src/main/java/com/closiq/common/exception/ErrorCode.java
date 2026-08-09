package com.closiq.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "One or more fields are invalid."),
    INVALID_OTP(HttpStatus.BAD_REQUEST, "OTP is invalid or expired."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access token has expired."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found."),
    PHONE_NOT_REGISTERED(HttpStatus.NOT_FOUND, "Phone number is not registered."),
    ALREADY_EXISTS(HttpStatus.CONFLICT, "Resource already exists."),
    SELLER_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Seller account is not verified."),
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient wallet balance."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later."),
    BOOKING_CONFLICT(HttpStatus.CONFLICT, "Selected dates are no longer available."),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "Idempotency key reused with different payload."),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "Action is not allowed in the current booking status."),
    PINCODE_NOT_SERVICEABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Delivery pincode is not serviceable."),
    MIN_RENTAL_PERIOD(HttpStatus.UNPROCESSABLE_ENTITY, "Rental period is shorter than the minimum allowed."),
    PAYMENT_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Payment verification failed."),
    COUPON_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon is invalid or expired."),
    PAYMENT_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Payment provider is temporarily unavailable."),
    LOGISTICS_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Logistics provider is temporarily unavailable."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");

    private final HttpStatus httpStatus;
    private final String defaultDetail;

    ErrorCode(HttpStatus httpStatus, String defaultDetail) {
        this.httpStatus = httpStatus;
        this.defaultDetail = defaultDetail;
    }
}
