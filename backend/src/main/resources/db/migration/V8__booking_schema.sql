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
