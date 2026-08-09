package com.closiq.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "closiq")
@Getter
@Setter
public class ClosiqProperties {

    private Jwt jwt = new Jwt();
    private Auth auth = new Auth();
    private Otp otp = new Otp();
    private Cors cors = new Cors();
    private S3 s3 = new S3();
    private Cloudinary cloudinary = new Cloudinary();
    private Storage storage = new Storage();
    private Inventory inventory = new Inventory();
    private Booking booking = new Booking();
    private Razorpay razorpay = new Razorpay();
    private Shadowfax shadowfax = new Shadowfax();
    private Mail mail = new Mail();

    @Getter
    @Setter
    public static class Razorpay {
        private String keyId = "rzp_test_stub";
        private String keySecret = "razorpay_stub_secret";
        private boolean stubEnabled = true;
    }

    @Getter
    @Setter
    public static class Shadowfax {
        private String webhookSecret = "shadowfax_stub_webhook_secret";
        private boolean stubEnabled = true;
    }

    @Getter
    @Setter
    public static class Booking {
        private int holdTtlMinutes = 15;
        private int rentalLeadDays = 2;
        private long deliveryFeeDefault = 0;
        private int sellerAcceptSlaHours = 24;
        /** Platform commission in basis points (1500 = 15%). */
        private int commissionRateBps = 1500;
    }

    @Getter
    @Setter
    public static class Inventory {
        private int lowStockThreshold = 2;
        private int defaultAvailabilityDays = 90;
    }

    @Getter
    @Setter
    public static class S3 {
        private String bucket = "closiq-media";
        private String region = "ap-south-1";
        private String cdnBaseUrl = "https://cdn.closiq.com";
        private boolean stubEnabled = true;
    }

    @Getter
    @Setter
    public static class Storage {
        /** Active storage backend: cloudinary | s3 */
        private String provider = "cloudinary";
    }

    @Getter
    @Setter
    public static class Cloudinary {
        private String cloudName = "oyi2aun5";
        private String apiKey = "143936174754118";
        private String apiSecret;
        /** Root folder in Cloudinary (matches dashboard key name). */
        private String folder = "closiq";
        private boolean stubEnabled = true;
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private int accessTokenExpirationMinutes = 15;
        private int refreshTokenExpirationDays = 30;
    }

    @Getter
    @Setter
    public static class Auth {
        /** Set false for local HTTP (frontend on localhost). */
        private boolean refreshCookieSecure = true;
    }

    @Getter
    @Setter
    public static class Otp {
        private int length = 6;
        private int expirySeconds = 300;
        private int resendCooldownSeconds = 60;
        private int maxResendsPerSession = 3;
        private int maxVerifyAttempts = 5;
        private int lockoutMinutes = 15;
        private boolean consoleLogEnabled = false;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }

    @Getter
    @Setter
    public static class Mail {
        /** When false, emails are logged to console instead of sent via SMTP. */
        private boolean enabled = false;
        private String from = "noreply@closiq.com";
        private String fromName = "Closiq";
    }
}
