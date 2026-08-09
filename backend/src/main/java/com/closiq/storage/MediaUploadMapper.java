package com.closiq.storage;

import com.closiq.seller.web.dto.PresignedUploadResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps provider-neutral storage instructions to the public upload API contract.
 */
@Component
public class MediaUploadMapper {

    public PresignedUploadResponse toPresignedResponse(UUID uploadId, UploadInstruction instruction) {
        return PresignedUploadResponse.builder()
                .uploadId(uploadId.toString())
                .uploadUrl(instruction.getUploadUrl())
                .method(instruction.getMethod())
                .headers(instruction.getHeaders())
                .formFields(instruction.getFormFields())
                .expiresAt(instruction.getExpiresAt())
                .publicUrl(instruction.getFileUrl())
                .build();
    }
}
