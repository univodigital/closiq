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
