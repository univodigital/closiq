package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class PresignedUploadResponse {

    String uploadId;
    String uploadUrl;
    String method;
    Map<String, String> headers;
    Map<String, String> formFields;
    Instant expiresAt;
    /** Public delivery URL after upload completes. */
    String publicUrl;
}
