-- =============================================================================
-- Closiq — admin user (Supabase SQL editor)
-- Run after Flyway migrations. Safe to re-run (uses fixed UUID + ON CONFLICT).
-- =============================================================================
--
-- Login: +919876543299 (OTP printed in backend console when test profile is active)
-- Roles: CUSTOMER + ADMIN
--
-- =============================================================================

BEGIN;

INSERT INTO "user" (id, user_code, phone, phone_verified, email, email_verified, password_hash, status)
VALUES (
    'a1000001-0001-4001-8001-000000000099',
    'VST-USR-100099',
    '+919876543299',
    TRUE,
    'admin@closiq.com',
    TRUE,
    NULL,
    'ACTIVE'
)
ON CONFLICT (id) DO UPDATE SET
    user_code = EXCLUDED.user_code,
    phone = EXCLUDED.phone,
    phone_verified = EXCLUDED.phone_verified,
    email = EXCLUDED.email,
    email_verified = EXCLUDED.email_verified,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO user_profile (user_id, first_name, last_name, display_name, preferences)
VALUES (
    'a1000001-0001-4001-8001-000000000099',
    'Closiq',
    'Admin',
    'Closiq Admin',
    '{}'::jsonb
)
ON CONFLICT (user_id) DO UPDATE SET
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    display_name = EXCLUDED.display_name,
    updated_at = now();

INSERT INTO user_role (user_id, role_id) VALUES
    ('a1000001-0001-4001-8001-000000000099', 1),  -- CUSTOMER
    ('a1000001-0001-4001-8001-000000000099', 3)   -- ADMIN
ON CONFLICT (user_id, role_id) DO NOTHING;

COMMIT;
