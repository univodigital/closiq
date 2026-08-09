-- Seller booking management fields

ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS seller_prep_by TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS seller_notes TEXT,
    ADD COLUMN IF NOT EXISTS seller_accepted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_booking_seller_rental_start
    ON booking (seller_profile_id, rental_start_date ASC)
    WHERE status NOT IN ('CANCELLED', 'COMPLETED', 'DEPOSIT_REFUNDED');
