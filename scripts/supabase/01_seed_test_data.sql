-- =============================================================================
-- Closiq — dev/test seed data for Supabase PostgreSQL
-- =============================================================================
--
-- PREREQUISITE: Flyway migrations must have run (start the Spring Boot backend
-- once with Supabase as DATABASE_URL, or apply db/migration/*.sql manually).
-- Flyway already seeds roles, categories, brands, 3 products, inventory, coupons.
--
-- IMAGES: Unsplash URLs only (no Cloudinary / no S3 upload required).
--
-- TEST ACCOUNTS (phone OTP login — OTP logged in backend dev console):
--   Ananya  +919876543210  customer + seller (House of Meera)
--   Priya   +919876543211  customer
--   Rhea    +919876543212  customer
--   Vikram  +919876543213  seller (Atelier Noir)
--   Sanaya  +919876543214  seller (Studio Rhea)
--
-- Optional email/password (for reset-password flow testing):
--   Password: Password123!
--   BCrypt:   $2y$10$FR7TaijP09T2KwnAcl67gOGvd0Pi7G6DawxpU6WhEvfDJgR5Z6GjW
--
-- Run 00_cleanup_test_data.sql first if re-seeding.
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Users & profiles
-- ---------------------------------------------------------------------------

INSERT INTO "user" (id, user_code, phone, phone_verified, email, email_verified, password_hash, status) VALUES
    ('a1000001-0001-4001-8001-000000000001', 'VST-USR-100010', '+919876543210', TRUE, 'ananya@example.com', TRUE,
     '$2y$10$FR7TaijP09T2KwnAcl67gOGvd0Pi7G6DawxpU6WhEvfDJgR5Z6GjW', 'ACTIVE'),
    ('a1000001-0001-4001-8001-000000000002', 'VST-USR-100011', '+919876543211', TRUE, 'priya@example.com', FALSE, NULL, 'ACTIVE'),
    ('a1000001-0001-4001-8001-000000000003', 'VST-USR-100012', '+919876543212', TRUE, 'rhea@example.com', FALSE, NULL, 'ACTIVE'),
    ('a1000001-0001-4001-8001-000000000004', 'VST-USR-100013', '+919876543213', TRUE, 'vikram@atelier-noir.com', TRUE,
     '$2y$10$FR7TaijP09T2KwnAcl67gOGvd0Pi7G6DawxpU6WhEvfDJgR5Z6GjW', 'ACTIVE'),
    ('a1000001-0001-4001-8001-000000000005', 'VST-USR-100014', '+919876543214', TRUE, 'sanaya@studio-rhea.com', TRUE,
     '$2y$10$FR7TaijP09T2KwnAcl67gOGvd0Pi7G6DawxpU6WhEvfDJgR5Z6GjW', 'ACTIVE');

INSERT INTO user_profile (user_id, first_name, last_name, display_name, preferences) VALUES
    ('a1000001-0001-4001-8001-000000000001', 'Ananya', 'Sharma', 'Ananya S.',
     '{"size":"S","occasions":["wedding","party"],"notifications":{"push":true,"email":true,"sms":false}}'::jsonb),
    ('a1000001-0001-4001-8001-000000000002', 'Priya', 'Kapoor', 'Priya K.', '{}'::jsonb),
    ('a1000001-0001-4001-8001-000000000003', 'Rhea', 'Mehta', 'Rhea M.', '{}'::jsonb),
    ('a1000001-0001-4001-8001-000000000004', 'Vikram', 'Desai', 'Vikram D.', '{}'::jsonb),
    ('a1000001-0001-4001-8001-000000000005', 'Sanaya', 'Iyer', 'Sanaya I.', '{}'::jsonb);

INSERT INTO user_role (user_id, role_id) VALUES
    ('a1000001-0001-4001-8001-000000000001', 1),  -- CUSTOMER
    ('a1000001-0001-4001-8001-000000000001', 2),  -- SELLER
    ('a1000001-0001-4001-8001-000000000002', 1),
    ('a1000001-0001-4001-8001-000000000003', 1),
    ('a1000001-0001-4001-8001-000000000004', 2),
    ('a1000001-0001-4001-8001-000000000005', 2);

-- ---------------------------------------------------------------------------
-- 2. Addresses (Mumbai serviceable pincodes from Flyway V2)
-- ---------------------------------------------------------------------------

INSERT INTO address (id, user_id, label, line1, line2, city, state, pincode, is_default) VALUES
    ('a8000001-0001-4001-8001-000000000001', 'a1000001-0001-4001-8001-000000000001',
     'Home', '14 Altamount Road', 'Flat 3B', 'Mumbai', 'Maharashtra', '400026', TRUE),
    ('a8000001-0001-4001-8001-000000000002', 'a1000001-0001-4001-8001-000000000001',
     'Office', 'Bandra Kurla Complex', 'Tower 2, 18th floor', 'Mumbai', 'Maharashtra', '400051', FALSE),
    ('a8000001-0001-4001-8001-000000000003', 'a1000001-0001-4001-8001-000000000002',
     'Home', '22 Pali Hill', NULL, 'Mumbai', 'Maharashtra', '400058', TRUE),
    ('a8000001-0001-4001-8001-000000000004', 'a1000001-0001-4001-8001-000000000003',
     'Home', '8 Carter Road', 'Apt 12', 'Mumbai', 'Maharashtra', '400076', TRUE);

-- ---------------------------------------------------------------------------
-- 3. Seller profiles (ACTIVE — bypasses missing admin approve API)
-- ---------------------------------------------------------------------------

INSERT INTO seller_application (id, user_id, business_name, business_type, city, description, pan_number, status, reviewed_at) VALUES
    ('a3000001-0001-4001-8001-000000000001', 'a1000001-0001-4001-8001-000000000001',
     'House of Meera', 'PROPRIETORSHIP', 'Mumbai', 'Curated wedding and occasion wear.', 'ABCDE1234F', 'VERIFIED', now()),
    ('a3000001-0001-4001-8001-000000000002', 'a1000001-0001-4001-8001-000000000004',
     'Atelier Noir', 'PRIVATE_LIMITED', 'Mumbai', 'Evening wear and cocktail edits.', 'FGHIJ5678K', 'VERIFIED', now()),
    ('a3000001-0001-4001-8001-000000000003', 'a1000001-0001-4001-8001-000000000005',
     'Studio Rhea', 'INDIVIDUAL', 'Mumbai', 'Party and festival rentals.', 'KLMNO9012P', 'VERIFIED', now());

INSERT INTO seller_profile (id, user_id, business_name, status, avg_rating, city, application_id, business_type, description, pan_number) VALUES
    ('a2000001-0001-4001-8001-000000000001', 'a1000001-0001-4001-8001-000000000001',
     'House of Meera', 'ACTIVE', 4.9, 'Mumbai', 'a3000001-0001-4001-8001-000000000001', 'PROPRIETORSHIP',
     'Curated wedding and occasion wear.', 'ABCDE1234F'),
    ('a2000001-0001-4001-8001-000000000002', 'a1000001-0001-4001-8001-000000000004',
     'Atelier Noir', 'ACTIVE', 4.8, 'Mumbai', 'a3000001-0001-4001-8001-000000000002', 'PRIVATE_LIMITED',
     'Evening wear and cocktail edits.', 'FGHIJ5678K'),
    ('a2000001-0001-4001-8001-000000000003', 'a1000001-0001-4001-8001-000000000005',
     'Studio Rhea', 'ACTIVE', 4.7, 'Mumbai', 'a3000001-0001-4001-8001-000000000003', 'INDIVIDUAL',
     'Party and festival rentals.', 'KLMNO9012P');

-- Link Flyway catalog products → sellers + extra gallery images (Unsplash)
UPDATE product SET seller_profile_id = 'a2000001-0001-4001-8001-000000000001'
WHERE id = '44444444-4444-7444-8444-444444444401';
UPDATE product SET seller_profile_id = 'a2000001-0001-4001-8001-000000000002'
WHERE id = '44444444-4444-7444-8444-444444444402';
UPDATE product SET seller_profile_id = 'a2000001-0001-4001-8001-000000000003'
WHERE id = '44444444-4444-7444-8444-444444444403';
UPDATE product SET seller_profile_id = 'a2000001-0001-4001-8001-000000000002'
WHERE id = '44444444-4444-7444-8444-444444444404';
UPDATE product SET seller_profile_id = 'a2000001-0001-4001-8001-000000000001'
WHERE id = '44444444-4444-7444-8444-444444444405';

INSERT INTO product_image (id, product_id, image_url, alt_text, sort_order) VALUES
    ('a9000001-0001-4001-8001-000000000001', '44444444-4444-7444-8444-444444444401',
     'https://images.unsplash.com/photo-1533709752211-118fcaf03312?auto=format&fit=crop&w=800&q=80', 'Emerald saree drape', 2),
    ('a9000001-0001-4001-8001-000000000002', '44444444-4444-7444-8444-444444444402',
     'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=800&q=80', 'Ivory gown detail', 1),
    ('a9000001-0001-4001-8001-000000000003', '44444444-4444-7444-8444-444444444403',
     'https://images.unsplash.com/photo-1496747611176-843222e1e955?auto=format&fit=crop&w=800&q=80', 'Sequin dress back', 1)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Seller wallets & bank accounts
-- ---------------------------------------------------------------------------

INSERT INTO wallet (id, seller_profile_id, available_balance, pending_balance, total_earned, total_withdrawn, last_settled_at) VALUES
    ('a6000001-0001-4001-8001-000000000001', 'a2000001-0001-4001-8001-000000000001', 8500, 1200, 9700, 0, now() - interval '7 days'),
    ('a6000001-0001-4001-8001-000000000002', 'a2000001-0001-4001-8001-000000000002', 4200, 0, 4200, 0, NULL),
    ('a6000001-0001-4001-8001-000000000003', 'a2000001-0001-4001-8001-000000000003', 1500, 0, 1500, 0, NULL);

INSERT INTO wallet_transaction (wallet_id, txn_type, amount, balance_after, reference_type, reference_id, description) VALUES
    ('a6000001-0001-4001-8001-000000000001', 'CREDIT_EARNING', 3312, 3312, 'BOOKING', 'VST-RNT-20260625-0510', 'Rental earnings — emerald saree'),
    ('a6000001-0001-4001-8001-000000000001', 'DEBIT_COMMISSION', 585, 2727, 'BOOKING', 'VST-RNT-20260625-0510', 'Platform commission'),
    ('a6000001-0001-4001-8001-000000000001', 'CREDIT_EARNING', 5773, 8500, 'BOOKING', 'VST-RNT-20260625-0520', 'Rental earnings — completed booking');

INSERT INTO bank_account (id, seller_profile_id, account_holder_name, account_number_enc, account_number_last4, ifsc_code, bank_name, status, is_default) VALUES
    ('a6100001-0001-4001-8001-000000000001', 'a2000001-0001-4001-8001-000000000001',
     'Ananya Sharma', 'enc:demo-acct-0001', '0001', 'HDFC0001234', 'HDFC Bank', 'VERIFIED', TRUE),
    ('a6100001-0001-4001-8001-000000000002', 'a2000001-0001-4001-8001-000000000002',
     'Atelier Noir Pvt Ltd', 'enc:demo-acct-0002', '0002', 'ICIC0005678', 'ICICI Bank', 'VERIFIED', TRUE);

-- ---------------------------------------------------------------------------
-- 5. Wishlist
-- ---------------------------------------------------------------------------

INSERT INTO wishlist_item (user_id, product_id) VALUES
    ('a1000001-0001-4001-8001-000000000002', '44444444-4444-7444-8444-444444444402'),
    ('a1000001-0001-4001-8001-000000000002', '44444444-4444-7444-8444-444444444403');

-- ---------------------------------------------------------------------------
-- 6. Bookings at different lifecycle stages
--    Amounts in whole INR (matches Phase 3 API contract).
-- ---------------------------------------------------------------------------

SELECT setval('rental_number_seq', 520, false);
SELECT setval('order_number_seq', 520, false);

-- Checkout sessions first (booking_id linked after bookings exist)
INSERT INTO checkout_session (id, customer_id, status, expires_at, delivery_address_id, ready_for_payment) VALUES
    ('a5000001-0001-4001-8001-000000000001', 'a1000001-0001-4001-8001-000000000002', 'COMPLETED', now() + interval '1 day',
     'a8000001-0001-4001-8001-000000000003', TRUE),
    ('a5000001-0001-4001-8001-000000000002', 'a1000001-0001-4001-8001-000000000003', 'COMPLETED', now() + interval '1 day',
     'a8000001-0001-4001-8001-000000000004', TRUE),
    ('a5000001-0001-4001-8001-000000000003', 'a1000001-0001-4001-8001-000000000002', 'COMPLETED', now() + interval '1 day',
     'a8000001-0001-4001-8001-000000000003', TRUE),
    ('a5000001-0001-4001-8001-000000000004', 'a1000001-0001-4001-8001-000000000003', 'COMPLETED', now() + interval '1 day',
     'a8000001-0001-4001-8001-000000000004', TRUE);

INSERT INTO booking (
    id, rental_number, order_number, customer_id, seller_profile_id, delivery_address_id,
    checkout_session_id, status, rental_start_date, rental_end_date, rental_days,
    rental_amount, deposit_amount, discount_amount, delivery_fee, total_amount,
    includes_trial, confirmed_at, seller_accepted_at, seller_prep_by, seller_notes, completed_at
) VALUES
    -- TRIAL_READY
    ('a4000001-0001-4001-8001-000000000001', 'VST-RNT-20260810-0482', 'VST-ORD-20260810-0482',
     'a1000001-0001-4001-8001-000000000002', 'a2000001-0001-4001-8001-000000000001',
     'a8000001-0001-4001-8001-000000000003', 'a5000001-0001-4001-8001-000000000001',
     'TRIAL_READY', '2026-08-14', '2026-08-17', 3,
     3897, 3500, 0, 99, 7496, TRUE,
     '2026-08-10T10:00:00Z', '2026-08-10T12:00:00Z', '2026-08-13T18:00:00+05:30',
     'Customer requested blouse pins included', NULL),
    -- RENTAL_ACTIVE
    ('a4000001-0001-4001-8001-000000000002', 'VST-RNT-20260728-0391', 'VST-ORD-20260728-0391',
     'a1000001-0001-4001-8001-000000000003', 'a2000001-0001-4001-8001-000000000002',
     'a8000001-0001-4001-8001-000000000004', 'a5000001-0001-4001-8001-000000000002',
     'RENTAL_ACTIVE', '2026-08-02', '2026-08-04', 2,
     4300, 5000, 0, 99, 9399, TRUE,
     '2026-07-28T10:00:00Z', '2026-07-28T14:00:00Z', '2026-08-01T18:00:00+05:30', NULL, NULL),
    -- COMPLETED (for reviews + seller earnings history)
    ('a4000001-0001-4001-8001-000000000003', 'VST-RNT-20260625-0510', 'VST-ORD-20260625-0510',
     'a1000001-0001-4001-8001-000000000002', 'a2000001-0001-4001-8001-000000000001',
     'a8000001-0001-4001-8001-000000000003', 'a5000001-0001-4001-8001-000000000003',
     'COMPLETED', '2026-07-01', '2026-07-03', 3,
     3897, 3500, 500, 99, 6996, TRUE,
     '2026-06-25T10:00:00Z', '2026-06-25T12:00:00Z', NULL, NULL, '2026-07-10T10:00:00Z'),
    -- CONFIRMED (awaiting seller accept)
    ('a4000001-0001-4001-8001-000000000004', 'VST-RNT-20260805-0521', 'VST-ORD-20260805-0521',
     'a1000001-0001-4001-8001-000000000003', 'a2000001-0001-4001-8001-000000000003',
     'a8000001-0001-4001-8001-000000000004', 'a5000001-0001-4001-8001-000000000004',
     'CONFIRMED', '2026-09-01', '2026-09-03', 3,
     2850, 2500, 0, 99, 5449, TRUE,
     now() - interval '2 hours', NULL, NULL, NULL, NULL),
    -- RETURNED (tail lifecycle gap — stops here until inspection is built)
    ('a4000001-0001-4001-8001-000000000005', 'VST-RNT-20260710-0515', 'VST-ORD-20260710-0515',
     'a1000001-0001-4001-8001-000000000003', 'a2000001-0001-4001-8001-000000000001',
     'a8000001-0001-4001-8001-000000000004', NULL,
     'RETURNED', '2026-07-15', '2026-07-17', 3,
     3897, 3500, 0, 99, 7496, TRUE,
     '2026-07-10T10:00:00Z', '2026-07-10T12:00:00Z', NULL, NULL, NULL);

UPDATE checkout_session SET booking_id = 'a4000001-0001-4001-8001-000000000001'
WHERE id = 'a5000001-0001-4001-8001-000000000001';
UPDATE checkout_session SET booking_id = 'a4000001-0001-4001-8001-000000000002'
WHERE id = 'a5000001-0001-4001-8001-000000000002';
UPDATE checkout_session SET booking_id = 'a4000001-0001-4001-8001-000000000003'
WHERE id = 'a5000001-0001-4001-8001-000000000003';
UPDATE checkout_session SET booking_id = 'a4000001-0001-4001-8001-000000000004'
WHERE id = 'a5000001-0001-4001-8001-000000000004';

INSERT INTO booking_item (id, booking_id, product_id, product_variant_id, inventory_item_id, price_snapshot) VALUES
    ('a4100001-0001-4001-8001-000000000001', 'a4000001-0001-4001-8001-000000000001',
     '44444444-4444-7444-8444-444444444401', '55555555-5555-7555-8555-555555555502',
     '77777777-7777-7777-8777-777777777702',
     '{"pricePerDay":1299,"deposit":3500,"productTitle":"Emerald draped saree","variantLabel":"S","imageUrl":"https://images.unsplash.com/photo-1596783074918-c84cb06531ca?auto=format&fit=crop&w=1200&q=80"}'::jsonb),
    ('a4100001-0001-4001-8001-000000000002', 'a4000001-0001-4001-8001-000000000002',
     '44444444-4444-7444-8444-444444444402', '55555555-5555-7555-8555-555555555506',
     '77777777-7777-7777-8777-777777777706',
     '{"pricePerDay":2150,"deposit":5000,"productTitle":"Ivory bias-cut gown","variantLabel":"S","imageUrl":"https://images.unsplash.com/photo-1568252542512-9fe8fe9c87bb?auto=format&fit=crop&w=1200&q=80"}'::jsonb),
    ('a4100001-0001-4001-8001-000000000003', 'a4000001-0001-4001-8001-000000000003',
     '44444444-4444-7444-8444-444444444401', '55555555-5555-7555-8555-555555555503',
     '77777777-7777-7777-8777-777777777704',
     '{"pricePerDay":1299,"deposit":3500,"productTitle":"Emerald draped saree","variantLabel":"M","imageUrl":"https://images.unsplash.com/photo-1596783074918-c84cb06531ca?auto=format&fit=crop&w=1200&q=80"}'::jsonb),
    ('a4100001-0001-4001-8001-000000000004', 'a4000001-0001-4001-8001-000000000004',
     '44444444-4444-7444-8444-444444444403', '55555555-5555-7555-8555-555555555508',
     '77777777-7777-7777-8777-777777777709',
     '{"pricePerDay":950,"deposit":2500,"productTitle":"Sequin cocktail dress","variantLabel":"M","imageUrl":"https://images.unsplash.com/photo-1614251055880-ee96e4803393?auto=format&fit=crop&w=1200&q=80"}'::jsonb),
    ('a4100001-0001-4001-8001-000000000005', 'a4000001-0001-4001-8001-000000000005',
     '44444444-4444-7444-8444-444444444401', '55555555-5555-7555-8555-555555555502',
     '77777777-7777-7777-8777-777777777703',
     '{"pricePerDay":1299,"deposit":3500,"productTitle":"Emerald draped saree","variantLabel":"S","imageUrl":"https://images.unsplash.com/photo-1596783074918-c84cb06531ca?auto=format&fit=crop&w=1200&q=80"}'::jsonb);

INSERT INTO booking_timeline (booking_id, actor_id, status, label, occurred_at) VALUES
    ('a4000001-0001-4001-8001-000000000001', 'a1000001-0001-4001-8001-000000000002', 'CONFIRMED', 'Payment received', now() - interval '5 days'),
    ('a4000001-0001-4001-8001-000000000001', 'a1000001-0001-4001-8001-000000000001', 'SELLER_ACCEPTED', 'Seller accepted booking', now() - interval '4 days'),
    ('a4000001-0001-4001-8001-000000000001', NULL, 'TRIAL_READY', 'Outfit delivered — trial window open', now() - interval '1 day'),
    ('a4000001-0001-4001-8001-000000000002', 'a1000001-0001-4001-8001-000000000003', 'RENTAL_ACTIVE', 'Trial accepted — rental started', now() - interval '3 days'),
    ('a4000001-0001-4001-8001-000000000003', NULL, 'COMPLETED', 'Booking completed', '2026-07-10T10:00:00Z'),
    ('a4000001-0001-4001-8001-000000000005', NULL, 'RETURNED', 'Return delivered to seller', now() - interval '2 days');

INSERT INTO trial_session (id, booking_id, started_at, expires_at, outcome) VALUES
    ('a4200001-0001-4001-8001-000000000001', 'a4000001-0001-4001-8001-000000000001',
     now() - interval '30 minutes', now() + interval '15 minutes', 'PENDING'),
    ('a4200001-0001-4001-8001-000000000002', 'a4000001-0001-4001-8001-000000000002',
     '2026-08-02T14:00:00Z', '2026-08-02T14:15:00Z', 'ACCEPTED');

INSERT INTO inventory_reservation (id, inventory_item_id, booking_id, start_date, end_date, reservation_type, status) VALUES
    ('a4300001-0001-4001-8001-000000000001', '77777777-7777-7777-8777-777777777702',
     'a4000001-0001-4001-8001-000000000001', '2026-08-14', '2026-08-18', 'CONFIRMED', 'ACTIVE'),
    ('a4300001-0001-4001-8001-000000000002', '77777777-7777-7777-8777-777777777706',
     'a4000001-0001-4001-8001-000000000002', '2026-08-02', '2026-08-05', 'CONFIRMED', 'ACTIVE'),
    ('a4300001-0001-4001-8001-000000000003', '77777777-7777-7777-8777-777777777704',
     'a4000001-0001-4001-8001-000000000003', '2026-07-01', '2026-07-04', 'CONFIRMED', 'RELEASED'),
    ('a4300001-0001-4001-8001-000000000004', '77777777-7777-7777-8777-777777777709',
     'a4000001-0001-4001-8001-000000000004', '2026-09-01', '2026-09-04', 'CONFIRMED', 'ACTIVE'),
    ('a4300001-0001-4001-8001-000000000005', '77777777-7777-7777-8777-777777777703',
     'a4000001-0001-4001-8001-000000000005', '2026-07-15', '2026-07-18', 'CONFIRMED', 'ACTIVE');

UPDATE inventory_item SET status = 'RENTED'
WHERE id IN (
    '77777777-7777-7777-8777-777777777702',
    '77777777-7777-7777-8777-777777777706',
    '77777777-7777-7777-8777-777777777709',
    '77777777-7777-7777-8777-777777777703'
);
UPDATE inventory_item SET status = 'AVAILABLE'
WHERE id = '77777777-7777-7777-8777-777777777704';

-- ---------------------------------------------------------------------------
-- 7. Payments (CAPTURED for paid bookings)
-- ---------------------------------------------------------------------------

INSERT INTO payment (
    id, booking_id, customer_id, checkout_session_id, provider_order_id, provider_payment_id,
    amount, rental_component, deposit_component, discount_component, status, payment_method, captured_at
) VALUES
    ('a4400001-0001-4001-8001-000000000001', 'a4000001-0001-4001-8001-000000000001',
     'a1000001-0001-4001-8001-000000000002', 'a5000001-0001-4001-8001-000000000001',
     'order_test_482', 'pay_test_482', 7496, 3897, 3500, 0, 'CAPTURED', 'UPI', now() - interval '5 days'),
    ('a4400001-0001-4001-8001-000000000002', 'a4000001-0001-4001-8001-000000000002',
     'a1000001-0001-4001-8001-000000000003', 'a5000001-0001-4001-8001-000000000002',
     'order_test_391', 'pay_test_391', 9399, 4300, 5000, 0, 'CAPTURED', 'CARD', now() - interval '10 days'),
    ('a4400001-0001-4001-8001-000000000003', 'a4000001-0001-4001-8001-000000000003',
     'a1000001-0001-4001-8001-000000000002', 'a5000001-0001-4001-8001-000000000003',
     'order_test_510', 'pay_test_510', 6996, 3897, 3500, 500, 'CAPTURED', 'UPI', '2026-06-25T10:05:00Z'),
    ('a4400001-0001-4001-8001-000000000004', 'a4000001-0001-4001-8001-000000000004',
     'a1000001-0001-4001-8001-000000000003', 'a5000001-0001-4001-8001-000000000004',
     'order_test_521', 'pay_test_521', 5449, 2850, 2500, 0, 'CAPTURED', 'UPI', now() - interval '2 hours'),
    ('a4400001-0001-4001-8001-000000000005', 'a4000001-0001-4001-8001-000000000005',
     'a1000001-0001-4001-8001-000000000003', NULL,
     'order_test_515', 'pay_test_515', 7496, 3897, 3500, 0, 'CAPTURED', 'UPI', '2026-07-10T10:05:00Z');

INSERT INTO refund (id, payment_id, booking_id, initiated_by, refund_type, amount, status, processed_at) VALUES
    ('a4500001-0001-4001-8001-000000000001', 'a4400001-0001-4001-8001-000000000003',
     'a4000001-0001-4001-8001-000000000003', NULL, 'DEPOSIT', 3500, 'PROCESSED', '2026-07-10T11:00:00Z');

-- ---------------------------------------------------------------------------
-- 8. Shipments
-- ---------------------------------------------------------------------------

INSERT INTO shipment (
    id, booking_id, logistics_provider_id, destination_address_id, shipment_type,
    provider_shipment_id, tracking_number, status, pickup_scheduled_at, delivered_at
) VALUES
    ('a4600001-0001-4001-8001-000000000001', 'a4000001-0001-4001-8001-000000000001',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'a8000001-0001-4001-8001-000000000003', 'OUTBOUND',
     'sfx_out_482', 'SFX482001', 'DELIVERED', now() - interval '2 days', now() - interval '1 day'),
    ('a4600001-0001-4001-8001-000000000002', 'a4000001-0001-4001-8001-000000000002',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'a8000001-0001-4001-8001-000000000004', 'OUTBOUND',
     'sfx_out_391', 'SFX391001', 'DELIVERED', '2026-08-01T10:00:00Z', '2026-08-02T12:00:00Z'),
    ('a4600001-0001-4001-8001-000000000003', 'a4000001-0001-4001-8001-000000000005',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'a8000001-0001-4001-8001-000000000004', 'OUTBOUND',
     'sfx_out_515', 'SFX515001', 'DELIVERED', '2026-07-14T10:00:00Z', '2026-07-15T12:00:00Z'),
    ('a4600001-0001-4001-8001-000000000004', 'a4000001-0001-4001-8001-000000000005',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'a8000001-0001-4001-8001-000000000004', 'RETURN',
     'sfx_ret_515', 'SFX515RET', 'DELIVERED', '2026-07-18T10:00:00Z', now() - interval '2 days');

INSERT INTO shipment_event (shipment_id, status, label, occurred_at, provider_event_id) VALUES
    ('a4600001-0001-4001-8001-000000000001', 'DELIVERED', 'Delivered to customer', now() - interval '1 day', 'evt_482_del'),
    ('a4600001-0001-4001-8001-000000000002', 'DELIVERED', 'Delivered to customer', '2026-08-02T12:00:00Z', 'evt_391_del'),
    ('a4600001-0001-4001-8001-000000000004', 'DELIVERED', 'Returned to seller', now() - interval '2 days', 'evt_515_ret');

-- ---------------------------------------------------------------------------
-- 9. Reviews & notifications
-- ---------------------------------------------------------------------------

INSERT INTO review (
    id, author_id, product_id, seller_profile_id, booking_id,
    product_rating, seller_rating, title, body, status, published_at
) VALUES
    ('a4700001-0001-4001-8001-000000000001', 'a1000001-0001-4001-8001-000000000002',
     '44444444-4444-7444-8444-444444444401', 'a2000001-0001-4001-8001-000000000001',
     'a4000001-0001-4001-8001-000000000003', 5, 5,
     'Perfect fit for my sister''s sangeet', 'Silk quality was excellent and delivery was on time.', 'PUBLISHED',
     '2026-07-11T10:00:00Z');

INSERT INTO notification (id, user_id, notification_type, title, body, deep_link, is_read) VALUES
    ('a4800001-0001-4001-8001-000000000001', 'a1000001-0001-4001-8001-000000000002',
     'TRIAL_READY', 'Your trial is ready',
     'Emerald draped saree has been delivered. You have 15 minutes to try it on.',
     '/bookings/a4000001-0001-4001-8001-000000000001', FALSE),
    ('a4800001-0001-4001-8001-000000000002', 'a1000001-0001-4001-8001-000000000001',
     'SELLER_NEW_BOOKING', 'New booking request',
     'Priya K. booked Emerald draped saree for 14–17 Aug.',
     '/seller/bookings/a4000001-0001-4001-8001-000000000001', TRUE),
    ('a4800001-0001-4001-8001-000000000003', 'a1000001-0001-4001-8001-000000000003',
     'BOOKING_CONFIRMED', 'Booking confirmed',
     'Your payment for Ivory bias-cut gown was successful.',
     '/bookings/a4000001-0001-4001-8001-000000000002', TRUE);

COMMIT;

-- Quick sanity check (optional — comment out if your SQL client stops at first result set)
SELECT 'users' AS entity, count(*) AS rows FROM "user" WHERE id::text LIKE 'a1000001-%'
UNION ALL SELECT 'sellers', count(*) FROM seller_profile WHERE id::text LIKE 'a2000001-%'
UNION ALL SELECT 'bookings', count(*) FROM booking WHERE id::text LIKE 'a4000001-%'
UNION ALL SELECT 'payments', count(*) FROM payment WHERE id::text LIKE 'a4400001-%';
