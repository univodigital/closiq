-- Seller product management: idempotency for create listing

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64) UNIQUE;
