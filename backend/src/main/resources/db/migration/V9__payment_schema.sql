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

INSERT INTO coupon (id, code, discount_type, discount_value, valid_until) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'FIRST500', 'FIXED', 500, '2026-12-31T23:59:59Z');

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
