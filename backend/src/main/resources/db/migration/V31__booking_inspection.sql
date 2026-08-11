-- Deposit inspection deductions recorded on booking before refund

ALTER TABLE booking
    ADD COLUMN inspection_damage_deduction BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN inspection_late_fee BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN inspection_cleaning_fee BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN inspection_notes TEXT,
    ADD COLUMN inspection_completed_at TIMESTAMPTZ;
