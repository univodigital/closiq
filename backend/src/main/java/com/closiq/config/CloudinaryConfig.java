package com.closiq.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies {@code CLOUDINARY_URL} when individual {@code closiq.cloudinary.*} fields are unset.
 * Also logs a masked startup summary to simplify credential troubleshooting.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CloudinaryConfig {

    private static final Pattern CLOUDINARY_URL = Pattern.compile(
            "^cloudinary://(?<apiKey>[^:]+):(?<apiSecret>[^@]+)@(?<cloudName>[^/?#]+)(?:/(?<folder>[^?#]+))?$");

    private final ClosiqProperties properties;
    private final Environment environment;

    @PostConstruct
    void applyCloudinaryUrlFallback() {
        ClosiqProperties.Cloudinary cloudinary = properties.getCloudinary();
        applyFromUrlIfMissing(cloudinary);
        logConfiguredCloudinary(cloudinary);
    }

    void applyFromUrlIfMissing(ClosiqProperties.Cloudinary cloudinary) {
        if (hasValue(cloudinary.getCloudName())
                && hasValue(cloudinary.getApiKey())
                && hasValue(cloudinary.getApiSecret())) {
            return;
        }

        String url = firstNonBlank(
                environment.getProperty("CLOUDINARY_URL"),
                environment.getProperty("cloudinary.url"));
        if (!hasValue(url)) {
            return;
        }

        Matcher matcher = CLOUDINARY_URL.matcher(url.trim());
        if (!matcher.matches()) {
            log.warn("CLOUDINARY_URL is set but could not be parsed; use cloudinary://key:secret@cloud_name");
            return;
        }

        if (!hasValue(cloudinary.getCloudName())) {
            cloudinary.setCloudName(decodeUrlComponent(matcher.group("cloudName")));
        }
        if (!hasValue(cloudinary.getApiKey())) {
            cloudinary.setApiKey(decodeUrlComponent(matcher.group("apiKey")));
        }
        if (!hasValue(cloudinary.getApiSecret())) {
            cloudinary.setApiSecret(decodeUrlComponent(matcher.group("apiSecret")));
        }
        String folder = matcher.group("folder");
        if (!hasValue(cloudinary.getFolder()) && hasValue(folder)) {
            cloudinary.setFolder(decodeUrlComponent(folder));
        }
    }

    private void logConfiguredCloudinary(ClosiqProperties.Cloudinary cloudinary) {
        if (cloudinary.isStubEnabled()) {
            log.info("Cloudinary uploads: stub mode enabled");
            return;
        }

        if (!hasValue(cloudinary.getCloudName())
                || !hasValue(cloudinary.getApiKey())
                || !hasValue(cloudinary.getApiSecret())) {
            log.warn(
                    "Cloudinary stub-disabled but credentials are incomplete. Set CLOUDINARY_URL or "
                            + "CLOUDINARY_CLOUD_NAME + CLOUDINARY_API_KEY + CLOUDINARY_API_SECRET "
                            + "(or closiq.cloudinary.* / CLOSIQ_CLOUDINARY_* env vars).");
            return;
        }

        log.info(
                "Cloudinary uploads: cloud={} apiKey={} folder={}",
                cloudinary.getCloudName(),
                maskApiKey(cloudinary.getApiKey()),
                cloudinary.getFolder());
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasValue(value)) {
                return value;
            }
        }
        return null;
    }

    private static String decodeUrlComponent(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    static String maskApiKey(String apiKey) {
        if (!hasValue(apiKey)) {
            return "(unset)";
        }
        if (apiKey.length() <= 4) {
            return "****";
        }
        return "****" + apiKey.substring(apiKey.length() - 4);
    }
}
