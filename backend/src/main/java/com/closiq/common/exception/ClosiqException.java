package com.closiq.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClosiqException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;
    private final String detail;

    public ClosiqException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultDetail(), errorCode.getHttpStatus());
    }

    public ClosiqException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, errorCode.getHttpStatus());
    }

    public ClosiqException(ErrorCode errorCode, String detail, HttpStatus status) {
        super(detail);
        this.errorCode = errorCode;
        this.detail = detail;
        this.status = status;
    }
}
