-- =============================================================================
-- Closiq — reset dev/test rows (Supabase SQL editor)
-- Run BEFORE 01_seed_test_data.sql when re-seeding.
-- Does NOT touch Flyway catalog/inventory seeds (categories, products, etc.).
-- =============================================================================

BEGIN;

-- Bookings & downstream
DELETE FROM review_image WHERE review_id IN (
    SELECT id FROM review WHERE booking_id IN (
        SELECT id FROM booking WHERE id::text LIKE 'a4000001-%'
    )
);
DELETE FROM review WHERE booking_id IN (SELECT id FROM booking WHERE id::text LIKE 'a4000001-%');
DELETE FROM notification WHERE user_id::text LIKE 'a1000001-%';
DELETE FROM shipment_event WHERE shipment_id IN (
    SELECT id FROM shipment WHERE booking_id IN (SELECT id FROM booking WHERE id::text LIKE 'a4000001-%')
);
DELETE FROM shipment WHERE booking_id IN (SELECT id FROM booking WHERE id::text LIKE 'a4000001-%');
DELETE FROM refund WHERE booking_id IN (SELECT id FROM booking WHERE id::text LIKE 'a4000001-%');
DELETE FROM payment_attempt WHERE payment_id IN (
    SELECT id FROM payment WHERE booking_id IN (SELECT id FROM booking WHERE id::text LIKE 'a4000001-%')
);
DELETE FROM payment WHERE booking_id IN (SELECT id FROM booking WHERE id::text LIKE 'a4000001-%');
DELETE FROM trial_session WHERE booking_id IN (SELECT id FROM booking WHERE id::text LIKE 'a4000001-%');
DELETE FROM booking_timeline WHERE booking_id IN (SELECT id FROM booking WHERE id::text LIKE 'a4000001-%');
DELETE FROM booking_item WHERE booking_id IN (SELECT id FROM booking WHERE id::text LIKE 'a4000001-%');
DELETE FROM inventory_reservation WHERE booking_id IN (SELECT id FROM booking WHERE id::text LIKE 'a4000001-%');
DELETE FROM booking WHERE id::text LIKE 'a4000001-%';
DELETE FROM checkout_session WHERE id::text LIKE 'a5000001-%';

-- Seller module
DELETE FROM wallet_transaction WHERE wallet_id IN (SELECT id FROM wallet WHERE id::text LIKE 'a6000001-%');
DELETE FROM payout_request WHERE seller_profile_id IN (SELECT id FROM seller_profile WHERE id::text LIKE 'a2000001-%');
DELETE FROM wallet WHERE id::text LIKE 'a6000001-%';
DELETE FROM bank_account WHERE seller_profile_id IN (SELECT id FROM seller_profile WHERE id::text LIKE 'a2000001-%');
DELETE FROM kyc_document WHERE application_id IN (SELECT id FROM seller_application WHERE id::text LIKE 'a3000001-%');
DELETE FROM media_asset WHERE id::text LIKE 'a7000001-%';
DELETE FROM seller_application WHERE id::text LIKE 'a3000001-%';

-- Unlink demo products before removing seller profiles (FK)
UPDATE product SET seller_profile_id = NULL
WHERE seller_profile_id IN (SELECT id FROM seller_profile WHERE id::text LIKE 'a2000001-%');

-- Users
DELETE FROM wishlist_item WHERE user_id::text LIKE 'a1000001-%';
DELETE FROM address WHERE user_id::text LIKE 'a1000001-%';
DELETE FROM user_role WHERE user_id::text LIKE 'a1000001-%';
DELETE FROM user_profile WHERE user_id::text LIKE 'a1000001-%';
DELETE FROM seller_profile WHERE id::text LIKE 'a2000001-%';
DELETE FROM "user" WHERE id::text LIKE 'a1000001-%';

COMMIT;
