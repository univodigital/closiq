package com.closiq.storage;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * Provider-independent instructions for a client-side direct upload.
 * Maps to the public API {@code PresignedUploadResponse} at the controller boundary.
 */
@Value
@Builder
public class UploadInstruction {

    /** Logical storage key persisted on {@code MediaAsset}. */
    String storageKey;

    /** Public delivery URL after upload completes. */
    String fileUrl;

    String uploadUrl;
    String method;
    Map<String, String> headers;
    Map<String, String> formFields;
    Instant expiresAt;
}
