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
