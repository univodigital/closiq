package com.closiq.storage;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StoredUploadResult {

    String storageKey;
    String publicUrl;
}
