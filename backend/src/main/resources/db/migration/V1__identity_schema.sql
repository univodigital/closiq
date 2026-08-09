-- Identity domain: users, roles, OTP sessions, refresh tokens

CREATE TABLE role (
    id          SMALLINT PRIMARY KEY,
    code        VARCHAR(32) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE "user" (
    id              UUID PRIMARY KEY,
    phone           VARCHAR(15) NOT NULL,
    phone_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    email           VARCHAR(255),
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash   VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE UNIQUE INDEX uk_user_phone_active ON "user" (phone) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_user_email_active ON "user" (email) WHERE email IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_user_phone ON "user" (phone);
CREATE INDEX idx_user_status ON "user" (status);
CREATE INDEX idx_user_created_at ON "user" (created_at);

CREATE TABLE user_profile (
    user_id         UUID PRIMARY KEY REFERENCES "user" (id),
    first_name      VARCHAR(50) NOT NULL,
    last_name       VARCHAR(50) NOT NULL,
    display_name    VARCHAR(100),
    avatar_media_id UUID,
    bio             TEXT,
    preferences     JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_role (
    user_id     UUID NOT NULL REFERENCES "user" (id),
    role_id     SMALLINT NOT NULL REFERENCES role (id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE otp_session (
    id              UUID PRIMARY KEY,
    phone           VARCHAR(15) NOT NULL,
    otp_hash        VARCHAR(255) NOT NULL,
    purpose         VARCHAR(20) NOT NULL,
    attempts        SMALLINT NOT NULL DEFAULT 0,
    resend_count    SMALLINT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at      TIMESTAMPTZ NOT NULL,
    verified_at     TIMESTAMPTZ,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_otp_purpose CHECK (purpose IN ('REGISTER', 'LOGIN', 'RESET')),
    CONSTRAINT chk_otp_status CHECK (status IN ('PENDING', 'VERIFIED', 'EXPIRED', 'LOCKED'))
);

CREATE INDEX idx_otp_phone_status ON otp_session (phone, status);
CREATE INDEX idx_otp_expires_at ON otp_session (expires_at);

CREATE TABLE refresh_token (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES "user" (id),
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    family_id       UUID NOT NULL,
    device_info     VARCHAR(255),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(512),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at      TIMESTAMPTZ,
    CONSTRAINT chk_refresh_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);
CREATE INDEX idx_refresh_token_family ON refresh_token (family_id);
CREATE INDEX idx_refresh_token_expires ON refresh_token (expires_at);

CREATE TABLE password_reset_token (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES "user" (id),
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_user ON password_reset_token (user_id);

-- Seed roles
INSERT INTO role (id, code) VALUES
    (1, 'CUSTOMER'),
    (2, 'SELLER'),
    (3, 'ADMIN');
