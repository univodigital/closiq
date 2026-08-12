-- =============================================================================
-- Closiq dev schema — consolidated DDL (no catalog/demo seed data)
-- =============================================================================
-- Generated from backend/src/main/resources/db/migration/
--
-- INCLUDED:  V1–V4, V6, V8–V21 (schema only), V26–V33
-- EXCLUDED:  V5, V7, V22, V23, V24, V25 (demo catalog / seed / data fixes)
--
-- Required reference data (roles, pincodes, logistics provider) is included
-- inline where defined in schema migrations (V1, V2, V10).
-- Optional demo coupon in V9 is omitted.
--
-- Apply in Supabase SQL Editor on an empty database, then baseline Flyway at V33:
--   spring.flyway.baseline-on-migrate=true
--   spring.flyway.baseline-version=33
--
-- See backend/docs/DEV-DEPLOYMENT.md for full deployment guide.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- V1
-- Source: db/migration/V1__identity_schema.sql
-- -----------------------------------------------------------------------------

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

-- -----------------------------------------------------------------------------
-- V2
-- Source: db/migration/V2__user_module_schema.sql
-- -----------------------------------------------------------------------------

-- User module: addresses, serviceability, wishlist, minimal catalog/seller stubs

CREATE TABLE address (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES "user" (id),
    label           VARCHAR(20) NOT NULL,
    line1           VARCHAR(100) NOT NULL,
    line2           VARCHAR(100),
    city            VARCHAR(50) NOT NULL,
    state           VARCHAR(50) NOT NULL,
    pincode         VARCHAR(6) NOT NULL,
    country_code    VARCHAR(2) NOT NULL DEFAULT 'IN',
    latitude        DECIMAL(9, 6),
    longitude       DECIMAL(9, 6),
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_address_user_id ON address (user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_address_pincode ON address (pincode);

CREATE TABLE serviceable_pincode (
    pincode                 VARCHAR(6) PRIMARY KEY,
    city                    VARCHAR(50) NOT NULL,
    state                   VARCHAR(50) NOT NULL,
    estimated_delivery_days SMALLINT NOT NULL DEFAULT 1,
    launch_phase            VARCHAR(20) NOT NULL DEFAULT 'MUMBAI',
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_pincode_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE seller_profile (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL UNIQUE REFERENCES "user" (id),
    business_name   VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    avg_rating      DECIMAL(2, 1),
    city            VARCHAR(50),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_seller_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_seller_profile_user ON seller_profile (user_id);

-- Minimal product stub for wishlist (expanded by catalog module)
CREATE TABLE product (
    id              UUID PRIMARY KEY,
    slug            VARCHAR(255) NOT NULL UNIQUE,
    title           VARCHAR(255) NOT NULL,
    price_per_day   BIGINT NOT NULL,
    deposit_amount  BIGINT NOT NULL DEFAULT 0,
    currency_code   VARCHAR(3) NOT NULL DEFAULT 'INR',
    primary_image_url VARCHAR(512),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT chk_product_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_product_status ON product (status) WHERE deleted_at IS NULL;

CREATE TABLE wishlist_item (
    user_id     UUID NOT NULL REFERENCES "user" (id),
    product_id  UUID NOT NULL REFERENCES product (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, product_id)
);

CREATE INDEX idx_wishlist_user_created ON wishlist_item (user_id, created_at DESC);

-- Mumbai MVP serviceable pincodes
INSERT INTO serviceable_pincode (pincode, city, state, estimated_delivery_days, launch_phase) VALUES
    ('400001', 'Mumbai', 'Maharashtra', 1, 'MUMBAI'),
    ('400026', 'Mumbai', 'Maharashtra', 1, 'MUMBAI'),
    ('400051', 'Mumbai', 'Maharashtra', 1, 'MUMBAI'),
    ('400058', 'Mumbai', 'Maharashtra', 1, 'MUMBAI'),
    ('400076', 'Mumbai', 'Maharashtra', 1, 'MUMBAI');

-- -----------------------------------------------------------------------------
-- V3
-- Source: db/migration/V3__seller_module_schema.sql
-- -----------------------------------------------------------------------------

-- Seller module: applications, KYC, bank accounts, wallet, payouts

ALTER TABLE seller_profile
    ADD COLUMN IF NOT EXISTS application_id UUID,
    ADD COLUMN IF NOT EXISTS business_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS description VARCHAR(500),
    ADD COLUMN IF NOT EXISTS gst_number VARCHAR(15),
    ADD COLUMN IF NOT EXISTS pan_number VARCHAR(10);

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS seller_profile_id UUID REFERENCES seller_profile (id);

CREATE INDEX IF NOT EXISTS idx_product_seller_profile ON product (seller_profile_id);

CREATE TABLE seller_application (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES "user" (id),
    business_name       VARCHAR(100) NOT NULL,
    business_type       VARCHAR(30) NOT NULL,
    city                VARCHAR(50) NOT NULL,
    description         VARCHAR(500),
    gst_number          VARCHAR(15),
    pan_number          VARCHAR(10) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason    TEXT,
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_application_status CHECK (
        status IN ('DRAFT', 'PENDING', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED', 'SUSPENDED')
    ),
    CONSTRAINT chk_business_type CHECK (
        business_type IN ('INDIVIDUAL', 'PROPRIETORSHIP', 'PARTNERSHIP', 'PRIVATE_LIMITED')
    )
);

CREATE INDEX idx_seller_application_user ON seller_application (user_id);
CREATE INDEX idx_seller_application_status ON seller_application (status);

CREATE TABLE media_asset (
    id                  UUID PRIMARY KEY,
    uploaded_by         UUID NOT NULL REFERENCES "user" (id),
    s3_bucket           VARCHAR(255) NOT NULL,
    s3_key              VARCHAR(512) NOT NULL,
    original_filename   VARCHAR(255),
    mime_type           VARCHAR(100) NOT NULL,
    file_size_bytes     BIGINT,
    status              VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_media_status CHECK (status IN ('UPLOADED', 'ATTACHED', 'ORPHANED')),
    CONSTRAINT uk_media_s3 UNIQUE (s3_bucket, s3_key)
);

CREATE TABLE kyc_document (
    id                  UUID PRIMARY KEY,
    application_id      UUID NOT NULL REFERENCES seller_application (id),
    document_type       VARCHAR(30) NOT NULL,
    media_asset_id      UUID NOT NULL REFERENCES media_asset (id),
    status              VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    uploaded_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at         TIMESTAMPTZ,
    CONSTRAINT chk_kyc_type CHECK (
        document_type IN ('PAN', 'ADDRESS_PROOF', 'GST_CERTIFICATE', 'BANK_STATEMENT')
    ),
    CONSTRAINT chk_kyc_status CHECK (
        status IN ('UPLOADED', 'PENDING_REVIEW', 'APPROVED', 'REJECTED')
    )
);

CREATE INDEX idx_kyc_application ON kyc_document (application_id);

CREATE TABLE bank_account (
    id                      UUID PRIMARY KEY,
    seller_profile_id       UUID NOT NULL REFERENCES seller_profile (id),
    account_holder_name     VARCHAR(100) NOT NULL,
    account_number_enc      VARCHAR(512) NOT NULL,
    account_number_last4    VARCHAR(4) NOT NULL,
    ifsc_code               VARCHAR(11) NOT NULL,
    bank_name               VARCHAR(100),
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    is_default              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bank_status CHECK (
        status IN ('PENDING_VERIFICATION', 'VERIFIED', 'INACTIVE')
    )
);

CREATE INDEX idx_bank_account_seller ON bank_account (seller_profile_id);

CREATE TABLE wallet (
    id                  UUID PRIMARY KEY,
    seller_profile_id   UUID NOT NULL UNIQUE REFERENCES seller_profile (id),
    available_balance   BIGINT NOT NULL DEFAULT 0,
    pending_balance     BIGINT NOT NULL DEFAULT 0,
    total_earned        BIGINT NOT NULL DEFAULT 0,
    total_withdrawn     BIGINT NOT NULL DEFAULT 0,
    currency_code       VARCHAR(3) NOT NULL DEFAULT 'INR',
    last_settled_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE wallet_transaction (
    id                  BIGSERIAL PRIMARY KEY,
    wallet_id           UUID NOT NULL REFERENCES wallet (id),
    txn_type            VARCHAR(30) NOT NULL,
    amount              BIGINT NOT NULL,
    balance_after       BIGINT NOT NULL,
    reference_type      VARCHAR(50),
    reference_id        VARCHAR(100),
    description         VARCHAR(255),
    status              VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_wallet_txn_type CHECK (
        txn_type IN (
            'CREDIT_EARNING', 'DEBIT_COMMISSION', 'DEBIT_PAYOUT',
            'DEBIT_PENALTY', 'CREDIT_ADJUSTMENT'
        )
    )
);

CREATE INDEX idx_wallet_txn_wallet_created ON wallet_transaction (wallet_id, created_at DESC);

CREATE TABLE payout_request (
    id                  UUID PRIMARY KEY,
    seller_profile_id   UUID NOT NULL REFERENCES seller_profile (id),
    bank_account_id     UUID NOT NULL REFERENCES bank_account (id),
    amount              BIGINT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    idempotency_key     VARCHAR(64),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at        TIMESTAMPTZ,
    CONSTRAINT chk_payout_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE UNIQUE INDEX uk_payout_idempotency ON payout_request (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- -----------------------------------------------------------------------------
-- V4
-- Source: db/migration/V4__catalog_schema.sql
-- -----------------------------------------------------------------------------

-- Catalog module: categories, brands, full product schema, variants, images, offers

CREATE TABLE category (
    id              UUID PRIMARY KEY,
    parent_id       UUID REFERENCES category (id),
    slug            VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    image_url       VARCHAR(512),
    vertical_code   VARCHAR(30) NOT NULL DEFAULT 'CLOTHING',
    depth           SMALLINT NOT NULL DEFAULT 0,
    sort_order      SMALLINT NOT NULL DEFAULT 0,
    featured        BOOLEAN NOT NULL DEFAULT FALSE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_category_status CHECK (status IN ('ACTIVE', 'DEPRECATED'))
);

CREATE INDEX idx_category_parent ON category (parent_id);
CREATE INDEX idx_category_featured ON category (featured) WHERE status = 'ACTIVE';

CREATE TABLE brand (
    id          UUID PRIMARY KEY,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS category_id UUID,
    ADD COLUMN IF NOT EXISTS brand_id UUID,
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS min_rental_days SMALLINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS max_rental_days SMALLINT,
    ADD COLUMN IF NOT EXISTS cleaning_buffer_days SMALLINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS includes_trial BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS trial_duration_minutes SMALLINT NOT NULL DEFAULT 15,
    ADD COLUMN IF NOT EXISTS city VARCHAR(50),
    ADD COLUMN IF NOT EXISTS avg_rating DECIMAL(2, 1),
    ADD COLUMN IF NOT EXISTS review_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS trending BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ;

ALTER TABLE product
    ADD CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category (id),
    ADD CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES brand (id);

CREATE INDEX IF NOT EXISTS idx_product_category ON product (category_id);
CREATE INDEX IF NOT EXISTS idx_product_featured ON product (featured) WHERE deleted_at IS NULL AND status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_product_trending ON product (trending) WHERE deleted_at IS NULL AND status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_product_city ON product (city);
CREATE INDEX IF NOT EXISTS idx_product_price ON product (price_per_day);

CREATE TABLE product_variant (
    id              UUID PRIMARY KEY,
    product_id      UUID NOT NULL REFERENCES product (id),
    sku             VARCHAR(50) NOT NULL,
    variant_label   VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    sort_order      SMALLINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_variant_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT uk_variant_product_sku UNIQUE (product_id, sku)
);

CREATE INDEX idx_variant_product_id ON product_variant (product_id);

CREATE TABLE product_image (
    id              UUID PRIMARY KEY,
    product_id      UUID NOT NULL REFERENCES product (id),
    image_url       VARCHAR(512) NOT NULL,
    alt_text        VARCHAR(255),
    sort_order      SMALLINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_product_image_product ON product_image (product_id, sort_order);

CREATE TABLE tag (
    id          UUID PRIMARY KEY,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL
);

CREATE TABLE product_tag (
    product_id  UUID NOT NULL REFERENCES product (id),
    tag_id      UUID NOT NULL REFERENCES tag (id),
    PRIMARY KEY (product_id, tag_id)
);

CREATE TABLE promotional_offer (
    id              UUID PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    code            VARCHAR(50),
    discount_type   VARCHAR(20) NOT NULL,
    discount_value  BIGINT NOT NULL,
    image_url       VARCHAR(512),
    valid_from      TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_until     TIMESTAMPTZ NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_offer_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
    CONSTRAINT chk_offer_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED'))
);

CREATE INDEX idx_offer_valid ON promotional_offer (valid_until) WHERE status = 'ACTIVE';

-- -----------------------------------------------------------------------------
-- V6
-- Source: db/migration/V6__inventory_schema.sql
-- -----------------------------------------------------------------------------

-- Inventory module: physical units, reservations, blocks, audit history

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE inventory_item (
    id                  UUID PRIMARY KEY,
    product_variant_id  UUID NOT NULL REFERENCES product_variant (id),
    serial_number       VARCHAR(50) NOT NULL UNIQUE,
    internal_tag        VARCHAR(50),
    condition_grade     VARCHAR(20) NOT NULL DEFAULT 'EXCELLENT',
    status              VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    acquired_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    retired_at          TIMESTAMPTZ,
    retire_reason       VARCHAR(255),
    last_inspection_at  TIMESTAMPTZ,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_inv_item_status CHECK (status IN (
        'AVAILABLE', 'RESERVED', 'RENTED', 'IN_TRANSIT', 'MAINTENANCE', 'RETIRED')),
    CONSTRAINT chk_inv_item_condition CHECK (condition_grade IN (
        'NEW', 'EXCELLENT', 'GOOD', 'FAIR', 'DAMAGED'))
);

CREATE INDEX idx_inv_item_variant ON inventory_item (product_variant_id);
CREATE INDEX idx_inv_item_status ON inventory_item (status);

CREATE TABLE inventory_reservation (
    id                  UUID PRIMARY KEY,
    inventory_item_id   UUID NOT NULL REFERENCES inventory_item (id),
    booking_id          UUID,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    reservation_type    VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    hold_expires_at     TIMESTAMPTZ,
    buffer_reason       VARCHAR(50),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_reservation_type CHECK (reservation_type IN ('HOLD', 'CONFIRMED', 'BUFFER')),
    CONSTRAINT chk_reservation_status CHECK (status IN ('ACTIVE', 'RELEASED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_reservation_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_reservation_item_dates ON inventory_reservation (inventory_item_id, start_date, end_date);
CREATE INDEX idx_reservation_booking ON inventory_reservation (booking_id);
CREATE INDEX idx_reservation_hold_expires ON inventory_reservation (hold_expires_at)
    WHERE status = 'ACTIVE' AND reservation_type = 'HOLD';

ALTER TABLE inventory_reservation
    ADD CONSTRAINT ex_reservation_no_overlap
    EXCLUDE USING gist (
        inventory_item_id WITH =,
        daterange(start_date, end_date, '[]') WITH &&
    )
    WHERE (status = 'ACTIVE');

CREATE TABLE inventory_block (
    id                  UUID PRIMARY KEY,
    product_variant_id  UUID NOT NULL REFERENCES product_variant (id),
    inventory_item_id   UUID REFERENCES inventory_item (id),
    created_by          UUID NOT NULL REFERENCES "user" (id),
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    reason              VARCHAR(255),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_block_status CHECK (status IN ('ACTIVE', 'REMOVED')),
    CONSTRAINT chk_block_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_block_variant_dates ON inventory_block (product_variant_id, start_date, end_date);
CREATE INDEX idx_block_item_dates ON inventory_block (inventory_item_id, start_date, end_date);

CREATE TABLE inventory_history (
    id                  BIGSERIAL PRIMARY KEY,
    inventory_item_id   UUID NOT NULL REFERENCES inventory_item (id),
    actor_id            UUID REFERENCES "user" (id),
    event_type          VARCHAR(50) NOT NULL,
    from_status         VARCHAR(20),
    to_status           VARCHAR(20),
    payload             JSONB,
    occurred_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_inv_history_item_occurred ON inventory_history (inventory_item_id, occurred_at DESC);

-- -----------------------------------------------------------------------------
-- V8
-- Source: db/migration/V8__booking_schema.sql
-- -----------------------------------------------------------------------------

-- Booking module: holds, checkout sessions, timeline, trial

CREATE SEQUENCE booking_number_seq START 482;

CREATE TABLE checkout_session (
    id              UUID PRIMARY KEY,
    customer_id     UUID NOT NULL REFERENCES "user" (id),
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_checkout_session_status CHECK (status IN ('OPEN', 'COMPLETED', 'EXPIRED'))
);

CREATE INDEX idx_checkout_session_customer ON checkout_session (customer_id);

CREATE TABLE booking (
    id                      UUID PRIMARY KEY,
    booking_number          VARCHAR(20) NOT NULL UNIQUE,
    order_number            VARCHAR(20) UNIQUE,
    customer_id             UUID NOT NULL REFERENCES "user" (id),
    seller_profile_id       UUID REFERENCES seller_profile (id),
    delivery_address_id     UUID REFERENCES address (id),
    checkout_session_id     UUID REFERENCES checkout_session (id),
    status                  VARCHAR(30) NOT NULL,
    rental_start_date       DATE NOT NULL,
    rental_end_date         DATE NOT NULL,
    rental_days             SMALLINT NOT NULL,
    rental_amount           BIGINT NOT NULL,
    deposit_amount          BIGINT NOT NULL,
    discount_amount         BIGINT NOT NULL DEFAULT 0,
    delivery_fee            BIGINT NOT NULL DEFAULT 0,
    total_amount            BIGINT NOT NULL,
    currency_code           CHAR(3) NOT NULL DEFAULT 'INR',
    includes_trial          BOOLEAN NOT NULL DEFAULT TRUE,
    trial_duration_minutes  SMALLINT NOT NULL DEFAULT 15,
    customer_notes          VARCHAR(200),
    idempotency_key         VARCHAR(64) UNIQUE,
    hold_expires_at         TIMESTAMPTZ,
    confirmed_at            TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    cancel_reason           VARCHAR(50),
    cancel_comment          TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_booking_dates CHECK (rental_end_date >= rental_start_date)
);

CREATE INDEX idx_booking_customer ON booking (customer_id, created_at DESC);
CREATE INDEX idx_booking_seller ON booking (seller_profile_id);
CREATE INDEX idx_booking_status ON booking (status);
CREATE INDEX idx_booking_hold_expires ON booking (hold_expires_at)
    WHERE status = 'PENDING_PAYMENT';

CREATE TABLE booking_item (
    id                  UUID PRIMARY KEY,
    booking_id          UUID NOT NULL REFERENCES booking (id),
    product_id          UUID NOT NULL REFERENCES product (id),
    product_variant_id  UUID NOT NULL REFERENCES product_variant (id),
    inventory_item_id   UUID NOT NULL REFERENCES inventory_item (id),
    price_snapshot      JSONB NOT NULL,
    quantity            SMALLINT NOT NULL DEFAULT 1,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_booking_item_booking ON booking_item (booking_id);

CREATE TABLE booking_timeline (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      UUID NOT NULL REFERENCES booking (id),
    actor_id        UUID REFERENCES "user" (id),
    status          VARCHAR(30) NOT NULL,
    label           VARCHAR(255) NOT NULL,
    description     TEXT,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata        JSONB
);

CREATE INDEX idx_timeline_booking_occurred ON booking_timeline (booking_id, occurred_at DESC);

CREATE TABLE trial_session (
    id              UUID PRIMARY KEY,
    booking_id      UUID NOT NULL UNIQUE REFERENCES booking (id),
    started_at      TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    accepted_at     TIMESTAMPTZ,
    rejected_at     TIMESTAMPTZ,
    outcome         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason   VARCHAR(50),
    reject_comment  TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_trial_outcome CHECK (outcome IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED'))
);

ALTER TABLE inventory_reservation
    ADD CONSTRAINT fk_reservation_booking FOREIGN KEY (booking_id) REFERENCES booking (id);

ALTER TABLE checkout_session
    ADD COLUMN booking_id UUID REFERENCES booking (id);

-- -----------------------------------------------------------------------------
-- V9 (schema only — demo coupon INSERT omitted)
-- Source: db/migration/V9__payment_schema.sql
-- -----------------------------------------------------------------------------

-- Payment module: payments, attempts, refunds, coupons; checkout session extensions

CREATE TABLE coupon (
    id                      UUID PRIMARY KEY,
    code                    VARCHAR(50) NOT NULL UNIQUE,
    discount_type           VARCHAR(20) NOT NULL,
    discount_value          BIGINT NOT NULL,
    max_discount_amount     BIGINT,
    min_order_amount        BIGINT NOT NULL DEFAULT 0,
    valid_from              TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_until             TIMESTAMPTZ NOT NULL,
    usage_limit             INT,
    usage_count             INT NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_coupon_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
    CONSTRAINT chk_coupon_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'DISABLED'))
);

CREATE INDEX idx_coupon_code ON coupon (code);


ALTER TABLE checkout_session
    ADD COLUMN IF NOT EXISTS delivery_address_id UUID REFERENCES address (id),
    ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS discount_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ready_for_payment BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE payment (
    id                      UUID PRIMARY KEY,
    booking_id              UUID NOT NULL REFERENCES booking (id),
    customer_id             UUID NOT NULL REFERENCES "user" (id),
    checkout_session_id     UUID REFERENCES checkout_session (id),
    provider_code           VARCHAR(20) NOT NULL DEFAULT 'RAZORPAY',
    provider_order_id       VARCHAR(100) NOT NULL UNIQUE,
    provider_payment_id     VARCHAR(100) UNIQUE,
    amount                  BIGINT NOT NULL,
    rental_component        BIGINT NOT NULL,
    deposit_component       BIGINT NOT NULL,
    discount_component      BIGINT NOT NULL DEFAULT 0,
    currency_code           CHAR(3) NOT NULL DEFAULT 'INR',
    status                  VARCHAR(30) NOT NULL,
    payment_method          VARCHAR(30),
    idempotency_key         VARCHAR(64) UNIQUE,
    captured_at             TIMESTAMPTZ,
    failed_at               TIMESTAMPTZ,
    failure_reason          TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_payment_status CHECK (status IN (
        'CREATED', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED'))
);

CREATE INDEX idx_payment_booking ON payment (booking_id);
CREATE INDEX idx_payment_customer ON payment (customer_id, created_at DESC);
CREATE INDEX idx_payment_status ON payment (status);
CREATE INDEX idx_payment_provider_order ON payment (provider_order_id);

CREATE TABLE payment_attempt (
    id                  BIGSERIAL PRIMARY KEY,
    payment_id          UUID NOT NULL REFERENCES payment (id),
    provider_order_id   VARCHAR(100),
    error_code          VARCHAR(50),
    error_message       TEXT,
    attempted_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_attempt_payment ON payment_attempt (payment_id);

CREATE TABLE refund (
    id                  UUID PRIMARY KEY,
    payment_id          UUID NOT NULL REFERENCES payment (id),
    booking_id          UUID NOT NULL REFERENCES booking (id),
    initiated_by        UUID REFERENCES "user" (id),
    refund_type         VARCHAR(20) NOT NULL,
    amount              BIGINT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_refund_id  VARCHAR(100),
    reason              TEXT,
    initiated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at        TIMESTAMPTZ,
    expected_by         TIMESTAMPTZ,
    CONSTRAINT chk_refund_type CHECK (refund_type IN ('RENTAL', 'DEPOSIT', 'FULL', 'PARTIAL', 'PENALTY')),
    CONSTRAINT chk_refund_status CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_refund_payment ON refund (payment_id);
CREATE INDEX idx_refund_booking ON refund (booking_id);

-- -----------------------------------------------------------------------------
-- V10
-- Source: db/migration/V10__shipment_schema.sql
-- -----------------------------------------------------------------------------

-- Logistics / Shipment module: providers, shipments, events, webhooks

CREATE TABLE logistics_provider (
    id              UUID PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_logistics_provider_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

INSERT INTO logistics_provider (id, code, name) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'SHADOWFAX', 'Shadowfax');

CREATE TABLE shipment (
    id                      UUID PRIMARY KEY,
    booking_id              UUID NOT NULL REFERENCES booking (id),
    logistics_provider_id   UUID NOT NULL REFERENCES logistics_provider (id),
    origin_address_id       UUID REFERENCES address (id),
    destination_address_id  UUID REFERENCES address (id),
    shipment_type           VARCHAR(20) NOT NULL,
    provider_shipment_id    VARCHAR(100),
    tracking_number         VARCHAR(50),
    status                  VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    pickup_scheduled_at     TIMESTAMPTZ,
    pickup_time_slot        VARCHAR(20),
    picked_up_at            TIMESTAMPTZ,
    delivered_at            TIMESTAMPTZ,
    estimated_delivery_at     TIMESTAMPTZ,
    failure_reason          TEXT,
    agent_name              VARCHAR(100),
    agent_phone_masked      VARCHAR(20),
    handoff_notes           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_shipment_type CHECK (shipment_type IN ('OUTBOUND', 'RETURN')),
    CONSTRAINT chk_shipment_status CHECK (status IN (
        'CREATED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED', 'RETURNED_TO_SELLER')),
    CONSTRAINT uk_shipment_booking_type UNIQUE (booking_id, shipment_type)
);

CREATE INDEX idx_shipment_booking ON shipment (booking_id);
CREATE INDEX idx_shipment_tracking ON shipment (tracking_number);
CREATE INDEX idx_shipment_status ON shipment (status);

CREATE TABLE shipment_event (
    id                  BIGSERIAL PRIMARY KEY,
    shipment_id         UUID NOT NULL REFERENCES shipment (id),
    status              VARCHAR(30) NOT NULL,
    label               VARCHAR(255) NOT NULL,
    location            VARCHAR(255),
    occurred_at         TIMESTAMPTZ NOT NULL,
    provider_event_id   VARCHAR(100),
    raw_payload         JSONB,
    CONSTRAINT uk_shipment_event_provider UNIQUE (shipment_id, provider_event_id)
);

CREATE INDEX idx_shipment_event_shipment_occurred ON shipment_event (shipment_id, occurred_at ASC);

CREATE TABLE logistics_webhook_event (
    id                  BIGSERIAL PRIMARY KEY,
    provider_code       VARCHAR(30) NOT NULL,
    provider_event_id   VARCHAR(100) NOT NULL,
    payload             JSONB NOT NULL,
    processed_at        TIMESTAMPTZ,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_logistics_webhook_event UNIQUE (provider_code, provider_event_id)
);

-- -----------------------------------------------------------------------------
-- V11
-- Source: db/migration/V11__review_schema.sql
-- -----------------------------------------------------------------------------

-- Review module: verified purchase reviews and photo attachments

CREATE TABLE review (
    id                      UUID PRIMARY KEY,
    author_id               UUID NOT NULL REFERENCES "user" (id),
    product_id              UUID NOT NULL REFERENCES product (id),
    seller_profile_id       UUID REFERENCES seller_profile (id),
    booking_id              UUID NOT NULL REFERENCES booking (id),
    product_rating          SMALLINT NOT NULL,
    seller_rating           SMALLINT,
    title                   VARCHAR(255),
    body                    TEXT,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    is_verified_purchase    BOOLEAN NOT NULL DEFAULT TRUE,
    idempotency_key         VARCHAR(64) UNIQUE,
    published_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_review_product_rating CHECK (product_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_review_seller_rating CHECK (seller_rating IS NULL OR seller_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_review_status CHECK (status IN ('PENDING', 'PUBLISHED', 'HIDDEN', 'FLAGGED')),
    CONSTRAINT uk_review_booking_author UNIQUE (booking_id, author_id)
);

CREATE INDEX idx_review_product ON review (product_id, created_at DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_review_seller ON review (seller_profile_id, created_at DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_review_status ON review (status);

CREATE TABLE review_image (
    id              UUID PRIMARY KEY,
    review_id       UUID NOT NULL REFERENCES review (id) ON DELETE CASCADE,
    media_asset_id  UUID NOT NULL REFERENCES media_asset (id),
    sort_order      SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_review_image_asset UNIQUE (review_id, media_asset_id)
);

CREATE INDEX idx_review_image_review ON review_image (review_id, sort_order ASC);

-- -----------------------------------------------------------------------------
-- V12
-- Source: db/migration/V12__notification_schema.sql
-- -----------------------------------------------------------------------------

-- In-app notification feed

CREATE TABLE notification (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES "user" (id),
    notification_type   VARCHAR(40) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    body                TEXT NOT NULL,
    payload             JSONB,
    deep_link             VARCHAR(512),
    is_read               BOOLEAN NOT NULL DEFAULT FALSE,
    read_at               TIMESTAMPTZ,
    expires_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_notification_type CHECK (notification_type IN (
        'TRIAL_READY', 'OUT_FOR_DELIVERY', 'BOOKING_CONFIRMED', 'RETURN_SCHEDULED',
        'DEPOSIT_REFUNDED', 'SELLER_NEW_BOOKING', 'SELLER_PAYOUT', 'PROMOTION'))
);

CREATE INDEX idx_notification_user_read ON notification (user_id, is_read, created_at DESC);
CREATE INDEX idx_notification_created ON notification (user_id, created_at DESC);

-- -----------------------------------------------------------------------------
-- V13
-- Source: db/migration/V13__seller_product_idempotency.sql
-- -----------------------------------------------------------------------------

-- Seller product management: idempotency for create listing

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64) UNIQUE;

-- -----------------------------------------------------------------------------
-- V14
-- Source: db/migration/V14__seller_booking_fields.sql
-- -----------------------------------------------------------------------------

-- Seller booking management fields

ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS seller_prep_by TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS seller_notes TEXT,
    ADD COLUMN IF NOT EXISTS seller_accepted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_booking_seller_rental_start
    ON booking (seller_profile_id, rental_start_date ASC)
    WHERE status NOT IN ('CANCELLED', 'COMPLETED', 'DEPOSIT_REFUNDED');

-- -----------------------------------------------------------------------------
-- V15
-- Source: db/migration/V15__audit_columns.sql
-- -----------------------------------------------------------------------------

-- Align audit columns with AuditableEntity (Address, Product, SellerApplication)

ALTER TABLE address
    ADD COLUMN IF NOT EXISTS created_by UUID,
    ADD COLUMN IF NOT EXISTS updated_by UUID;

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS created_by UUID,
    ADD COLUMN IF NOT EXISTS updated_by UUID;

ALTER TABLE seller_application
    ADD COLUMN IF NOT EXISTS created_by UUID,
    ADD COLUMN IF NOT EXISTS updated_by UUID;

-- -----------------------------------------------------------------------------
-- V16
-- Source: db/migration/V16__currency_code_varchar.sql
-- -----------------------------------------------------------------------------

-- Hibernate maps @Column(length=3) to VARCHAR; align CHAR columns from early migrations.

ALTER TABLE booking
    ALTER COLUMN currency_code TYPE VARCHAR(3) USING currency_code::varchar;

ALTER TABLE payment
    ALTER COLUMN currency_code TYPE VARCHAR(3) USING currency_code::varchar;

-- -----------------------------------------------------------------------------
-- V17
-- Source: db/migration/V17__business_identifiers.sql
-- -----------------------------------------------------------------------------

-- Business identifiers: product_code, user_code; rental_number rename; VST-* formats

ALTER TABLE product ADD COLUMN IF NOT EXISTS product_code VARCHAR(32);
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS user_code VARCHAR(32);

ALTER TABLE booking RENAME COLUMN booking_number TO rental_number;
ALTER TABLE booking ALTER COLUMN rental_number TYPE VARCHAR(32);
ALTER TABLE booking ALTER COLUMN order_number TYPE VARCHAR(32);

CREATE SEQUENCE IF NOT EXISTS product_code_seq START 100001;
CREATE SEQUENCE IF NOT EXISTS user_code_seq START 100001;
CREATE SEQUENCE IF NOT EXISTS order_number_seq START 1;
CREATE SEQUENCE IF NOT EXISTS rental_number_seq START 1;

-- Backfill product codes
WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn FROM product
)
UPDATE product p
SET product_code = 'VST-PROD-' || LPAD(n.rn::text, 6, '0')
FROM numbered n
WHERE p.id = n.id AND p.product_code IS NULL;

SELECT setval('product_code_seq', GREATEST(100001, COALESCE(
    (SELECT MAX(CAST(SUBSTRING(product_code FROM 10) AS BIGINT)) FROM product WHERE product_code LIKE 'VST-PROD-%'), 100000
) + 1));

-- Backfill user codes
WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn FROM "user"
)
UPDATE "user" u
SET user_code = 'VST-USR-' || LPAD(n.rn::text, 6, '0')
FROM numbered n
WHERE u.id = n.id AND u.user_code IS NULL;

SELECT setval('user_code_seq', GREATEST(100001, COALESCE(
    (SELECT MAX(CAST(SUBSTRING(user_code FROM 9) AS BIGINT)) FROM "user" WHERE user_code LIKE 'VST-USR-%'), 100000
) + 1));

-- Backfill rental and order numbers to VST-* format
WITH numbered AS (
    SELECT id,
           ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn,
           TO_CHAR(created_at AT TIME ZONE 'UTC', 'YYYYMMDD') AS dt
    FROM booking
)
UPDATE booking b
SET rental_number = 'VST-RNT-' || n.dt || '-' || LPAD(n.rn::text, 4, '0'),
    order_number = 'VST-ORD-' || n.dt || '-' || LPAD(n.rn::text, 4, '0')
FROM numbered n
WHERE b.id = n.id;

SELECT setval('rental_number_seq', GREATEST(1, (SELECT COUNT(*) FROM booking) + 1));
SELECT setval('order_number_seq', GREATEST(1, (SELECT COUNT(*) FROM booking) + 1));

ALTER TABLE product ALTER COLUMN product_code SET NOT NULL;
ALTER TABLE "user" ALTER COLUMN user_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_product_code ON product (product_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_user_code ON "user" (user_code);

DROP SEQUENCE IF EXISTS booking_number_seq;

-- -----------------------------------------------------------------------------
-- V18
-- Source: db/migration/V18__user_alternate_contact.sql
-- -----------------------------------------------------------------------------

ALTER TABLE "user" ADD COLUMN IF NOT EXISTS alternate_phone VARCHAR(15);
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS alternate_email VARCHAR(255);

-- -----------------------------------------------------------------------------
-- V19
-- Source: db/migration/V19__media_asset_storage_provider.sql
-- -----------------------------------------------------------------------------

ALTER TABLE media_asset
    ADD COLUMN IF NOT EXISTS storage_provider VARCHAR(20);

UPDATE media_asset
SET storage_provider = 'CLOUDINARY'
WHERE storage_provider IS NULL;

ALTER TABLE media_asset
    ALTER COLUMN storage_provider SET NOT NULL;

ALTER TABLE media_asset
    ALTER COLUMN storage_provider SET DEFAULT 'CLOUDINARY';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_media_storage_provider'
    ) THEN
        ALTER TABLE media_asset
            ADD CONSTRAINT chk_media_storage_provider
                CHECK (storage_provider IN ('CLOUDINARY', 'S3'));
    END IF;
END $$;

COMMENT ON COLUMN media_asset.s3_bucket IS 'Provider namespace: Cloudinary cloud name or S3 bucket';
COMMENT ON COLUMN media_asset.s3_key IS 'Logical storage key (provider-neutral)';
COMMENT ON COLUMN media_asset.storage_provider IS 'Storage backend that owns this object';

-- -----------------------------------------------------------------------------
-- V20
-- Source: db/migration/V20__address_phone.sql
-- -----------------------------------------------------------------------------

ALTER TABLE address
    ADD COLUMN phone VARCHAR(15);

UPDATE address a
SET phone = u.phone
FROM "user" u
WHERE a.user_id = u.id
  AND a.phone IS NULL;

ALTER TABLE address
    ALTER COLUMN phone SET NOT NULL;

-- -----------------------------------------------------------------------------
-- V21 (schema only — seed INSERTs omitted)
-- Source: db/migration/V21__product_audience.sql
-- -----------------------------------------------------------------------------

ALTER TABLE product
    ADD COLUMN audience VARCHAR(10),
    ADD COLUMN garment_type VARCHAR(50);

ALTER TABLE product
    ALTER COLUMN audience SET NOT NULL;

-- -----------------------------------------------------------------------------
-- V26
-- Source: db/migration/V26__user_username.sql
-- -----------------------------------------------------------------------------

-- Add unique username to user profiles for password-based login

ALTER TABLE user_profile ADD COLUMN username VARCHAR(30);

CREATE UNIQUE INDEX uk_user_profile_username ON user_profile (LOWER(username))
    WHERE username IS NOT NULL;

-- -----------------------------------------------------------------------------
-- V27
-- Source: db/migration/V27__user_profile_gender.sql
-- -----------------------------------------------------------------------------

ALTER TABLE user_profile
    ADD COLUMN gender VARCHAR(20);

UPDATE user_profile
SET gender = 'PREFER_NOT_TO_SAY'
WHERE gender IS NULL;

ALTER TABLE user_profile
    ALTER COLUMN gender SET NOT NULL;

-- -----------------------------------------------------------------------------
-- V28
-- Source: db/migration/V28__account_security.sql
-- -----------------------------------------------------------------------------

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

-- -----------------------------------------------------------------------------
-- V29
-- Source: db/migration/V29__cart_and_checkout_batch.sql
-- -----------------------------------------------------------------------------

-- Server-side cart for authenticated users and multi-item checkout batches.

CREATE TABLE cart_item (
    id                  UUID PRIMARY KEY,
    customer_id         UUID NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    product_slug        VARCHAR(255) NOT NULL,
    variant_size        VARCHAR(20) NOT NULL,
    rental_start_date   DATE NOT NULL,
    rental_end_date     DATE NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_cart_item_customer_slug UNIQUE (customer_id, product_slug),
    CONSTRAINT chk_cart_item_dates CHECK (rental_end_date >= rental_start_date)
);

CREATE INDEX idx_cart_item_customer ON cart_item (customer_id);

CREATE TABLE checkout_batch (
    id                      UUID PRIMARY KEY,
    customer_id             UUID NOT NULL REFERENCES "user" (id),
    delivery_address_id     UUID REFERENCES address (id),
    coupon_code             VARCHAR(50),
    discount_amount         BIGINT NOT NULL DEFAULT 0,
    total_amount            BIGINT NOT NULL,
    currency_code           CHAR(3) NOT NULL DEFAULT 'INR',
    status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    expires_at              TIMESTAMPTZ NOT NULL,
    idempotency_key         VARCHAR(64) UNIQUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_checkout_batch_status CHECK (status IN ('OPEN', 'COMPLETED', 'EXPIRED', 'FAILED'))
);

CREATE INDEX idx_checkout_batch_customer ON checkout_batch (customer_id, created_at DESC);

ALTER TABLE booking
    ADD COLUMN checkout_batch_id UUID REFERENCES checkout_batch (id);

CREATE INDEX idx_booking_checkout_batch ON booking (checkout_batch_id)
    WHERE checkout_batch_id IS NOT NULL;

ALTER TABLE payment
    ADD COLUMN checkout_batch_id UUID REFERENCES checkout_batch (id);

CREATE INDEX idx_payment_checkout_batch ON payment (checkout_batch_id)
    WHERE checkout_batch_id IS NOT NULL;

-- -----------------------------------------------------------------------------
-- V30
-- Source: db/migration/V30__refund_idempotency.sql
-- -----------------------------------------------------------------------------

-- Refund idempotency for safe duplicate prevention

ALTER TABLE refund
    ADD COLUMN idempotency_key VARCHAR(64) UNIQUE;

-- -----------------------------------------------------------------------------
-- V31
-- Source: db/migration/V31__booking_inspection.sql
-- -----------------------------------------------------------------------------

-- Deposit inspection deductions recorded on booking before refund

ALTER TABLE booking
    ADD COLUMN inspection_damage_deduction BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN inspection_late_fee BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN inspection_cleaning_fee BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN inspection_notes TEXT,
    ADD COLUMN inspection_completed_at TIMESTAMPTZ;

-- -----------------------------------------------------------------------------
-- V32
-- Source: db/migration/V32__category_name_unique.sql
-- -----------------------------------------------------------------------------

-- Case-insensitive unique category names (slug remains the canonical unique key).
CREATE UNIQUE INDEX IF NOT EXISTS uk_category_name_lower ON category (LOWER(name));

-- -----------------------------------------------------------------------------
-- V33
-- Source: db/migration/V33__checkout_batch_currency_varchar.sql
-- -----------------------------------------------------------------------------

-- Hibernate maps @Column(length=3) to VARCHAR; V29 used CHAR(3) for checkout_batch.

ALTER TABLE checkout_batch
    ALTER COLUMN currency_code TYPE VARCHAR(3) USING currency_code::varchar;

-- =============================================================================
-- End of closiq-dev-schema.sql
-- =============================================================================
