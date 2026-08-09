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
