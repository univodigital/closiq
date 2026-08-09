package com.closiq.storage.cloudinary;

import com.closiq.config.ClosiqProperties;
import com.closiq.storage.FileStorageService;
import com.closiq.storage.StorageProvider;
import com.closiq.storage.UploadInstruction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryFileStorageServiceTest {

    private CloudinaryFileStorageService service;

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        properties.getCloudinary().setCloudName("test-cloud");
        properties.getCloudinary().setFolder("closiq");
        properties.getCloudinary().setApiKey("key");
        properties.getCloudinary().setApiSecret("secret");
        properties.getCloudinary().setStubEnabled(true);
        service = new CloudinaryFileStorageService(properties);
    }

    @Test
    void provider_isCloudinary() {
        assertThat(service.provider()).isEqualTo(StorageProvider.CLOUDINARY);
    }

    @Test
    void buildStorageKey_prefixesLogicalPath() {
        assertThat(service.buildStorageKey("kyc/user-1/pan/id-1"))
                .isEqualTo("closiq/kyc/user-1/pan/id-1");
    }

    @Test
    void resolvePublicUrl_doesNotLeakProviderTypesOutsideInterface() {
        String url = service.resolvePublicUrl("closiq/products/p-1/img-1", "image/jpeg");
        assertThat(url).startsWith("https://res.cloudinary.com/test-cloud/image/upload/v1/");
    }

    @Test
    void createUploadInstruction_returnsProviderNeutralFields() {
        UploadInstruction instruction = service.createUploadInstruction(
                "products/p-1/img-1", "image/jpeg");

        assertThat(instruction.getStorageKey()).isEqualTo("closiq/products/p-1/img-1");
        assertThat(instruction.getFileUrl()).contains("closiq/products/p-1/img-1");
        assertThat(instruction.getUploadUrl()).isNotBlank();
    }

    @Test
    void implementsFileStorageService() {
        assertThat(service).isInstanceOf(FileStorageService.class);
    }
}
