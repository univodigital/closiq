package com.closiq.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetailResponse {

    String type;
    String title;
    int status;
    String code;
    String detail;
    String instance;
    String requestId;
    Instant timestamp;
    List<FieldErrorItem> errors;

    @Value
    @Builder
    public static class FieldErrorItem {
        String field;
        String code;
        String message;
        Object rejectedValue;
    }
}
