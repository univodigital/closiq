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
