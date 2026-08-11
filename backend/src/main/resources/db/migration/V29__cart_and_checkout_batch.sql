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
