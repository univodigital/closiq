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
