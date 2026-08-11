package com.closiq.config;

import com.closiq.config.ClosiqProperties.Cloudinary;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryConfigTest {

    @Test
    void parseCloudinaryUrl_populatesMissingCredentials() {
        ClosiqProperties properties = new ClosiqProperties();
        Cloudinary cloudinary = properties.getCloudinary();
        cloudinary.setStubEnabled(false);

        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(
                "CLOUDINARY_URL",
                "cloudinary://123456789012345:my-secret-with-dash@demo-cloud/closiq");

        CloudinaryConfig config = new CloudinaryConfig(properties, environment);
        config.applyFromUrlIfMissing(cloudinary);

        assertThat(cloudinary.getCloudName()).isEqualTo("demo-cloud");
        assertThat(cloudinary.getApiKey()).isEqualTo("123456789012345");
        assertThat(cloudinary.getApiSecret()).isEqualTo("my-secret-with-dash");
        assertThat(cloudinary.getFolder()).isEqualTo("closiq");
    }

    @Test
    void parseCloudinaryUrl_doesNotOverrideExplicitProperties() {
        ClosiqProperties properties = new ClosiqProperties();
        Cloudinary cloudinary = properties.getCloudinary();
        cloudinary.setCloudName("configured-cloud");
        cloudinary.setApiKey("configured-key");
        cloudinary.setApiSecret("configured-secret");

        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("CLOUDINARY_URL", "cloudinary://other:secret@other-cloud");

        CloudinaryConfig config = new CloudinaryConfig(properties, environment);
        config.applyFromUrlIfMissing(cloudinary);

        assertThat(cloudinary.getCloudName()).isEqualTo("configured-cloud");
        assertThat(cloudinary.getApiKey()).isEqualTo("configured-key");
        assertThat(cloudinary.getApiSecret()).isEqualTo("configured-secret");
    }

    @Test
    void maskApiKey_showsLastFourDigits() {
        assertThat(CloudinaryConfig.maskApiKey("143936174754118")).isEqualTo("****4118");
    }
}
