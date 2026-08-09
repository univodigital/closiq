package com.closiq.common.exception;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FieldErrorDetail {

    String field;
    String code;
    String message;
    Object rejectedValue;
}
