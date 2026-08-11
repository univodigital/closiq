-- Refund idempotency for safe duplicate prevention

ALTER TABLE refund
    ADD COLUMN idempotency_key VARCHAR(64) UNIQUE;
