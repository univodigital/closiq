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
