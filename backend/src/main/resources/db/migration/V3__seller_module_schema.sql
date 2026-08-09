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
