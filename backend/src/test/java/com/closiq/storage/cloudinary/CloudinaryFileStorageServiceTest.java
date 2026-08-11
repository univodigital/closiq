package com.closiq.storage.cloudinary;

import com.closiq.config.ClosiqProperties;
import com.closiq.storage.FileStorageService;
import com.closiq.storage.StorageProvider;
import com.closiq.storage.StoredUploadResult;
import com.closiq.storage.UploadInstruction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryFileStorageServiceTest {

    private CloudinaryFileStorageService service;
    private ClosiqProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ClosiqProperties();
        properties.getCloudinary().setCloudName("test-cloud");
        properties.getCloudinary().setFolder("closiq");
        properties.getCloudinary().setApiKey("key");
        properties.getCloudinary().setApiSecret("secret");
        properties.getCloudinary().setStubEnabled(true);
        service = new CloudinaryFileStorageService(properties, new ObjectMapper());
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
        assertThat(url).isEqualTo(
                "https://res.cloudinary.com/test-cloud/image/upload/closiq/products/p-1/img-1");
    }

    @Test
    void signUploadParams_matchesCloudinaryDocumentationExample() {
        Map<String, String> params = new TreeMap<>();
        params.put("eager", "w_400,h_300,c_pad|w_260,h_200,c_crop");
        params.put("public_id", "sample_image");
        params.put("timestamp", "1315060510");

        assertThat(CloudinaryFileStorageService.signUploadParams(params, "abcd"))
                .isEqualTo("bfd09f95f331f558cbd1320e67aa8d488770583e");
    }

    @Test
    void uploadBytes_inStubMode_returnsPredictedUrl() {
        StoredUploadResult result = service.uploadBytes(
                "products/p-1/img-1", "image/jpeg", "photo.jpg", new byte[] {1, 2, 3});

        assertThat(result.getStorageKey()).isEqualTo("closiq/products/p-1/img-1");
        assertThat(result.getPublicUrl())
                .isEqualTo("https://res.cloudinary.com/test-cloud/image/upload/closiq/products/p-1/img-1");
    }

    @Test
    void createUploadInstruction_withCredentials_usesImageUploadEndpoint() {
        properties.getCloudinary().setStubEnabled(false);
        properties.getCloudinary().setApiSecret("secret");

        UploadInstruction instruction = service.createUploadInstruction(
                "products/p-1/img-1", "image/jpeg");

        assertThat(instruction.getUploadUrl())
                .isEqualTo("https://api.cloudinary.com/v1_1/test-cloud/image/upload");
        assertThat(instruction.getFormFields()).containsKeys(
                "api_key", "timestamp", "signature", "public_id");
        assertThat(instruction.getFormFields())
                .doesNotContainKey("folder");
        assertThat(instruction.getFormFields().get("public_id"))
                .isEqualTo("closiq/products/p-1/img-1");
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
