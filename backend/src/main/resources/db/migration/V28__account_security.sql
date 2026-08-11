-- Account security: pending email, username change tracking, extended OTP purposes

ALTER TABLE "user"
    ADD COLUMN pending_email VARCHAR(255);

ALTER TABLE user_profile
    ADD COLUMN username_changed_at TIMESTAMPTZ;

ALTER TABLE otp_session DROP CONSTRAINT chk_otp_purpose;

ALTER TABLE otp_session ADD CONSTRAINT chk_otp_purpose
    CHECK (purpose IN (
        'REGISTER',
        'LOGIN',
        'RESET',
        'CHANGE_PHONE_OLD',
        'CHANGE_PHONE_NEW',
        'CHANGE_EMAIL'
    ));
