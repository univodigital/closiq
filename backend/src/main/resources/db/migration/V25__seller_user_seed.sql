-- Seed seller user profiles for Flyway catalog products (V5 + V21).
-- OTP dev login — use these phones; OTP is logged in the backend console.
--
--   Ananya  +919876543210  House of Meera   (emerald saree, kids lehenga)
--   Vikram  +919876543213  Atelier Noir     (ivory gown, sherwani)
--   Sanaya  +919876543214  Studio Rhea      (sequin cocktail dress)
--
-- Optional customer for checkout testing:
--   Priya   +919876543211

INSERT INTO "user" (id, user_code, phone, phone_verified, email, email_verified, status) VALUES
    ('a1000001-0001-4001-8001-000000000001', 'VST-USR-100010', '+919876543210', TRUE, 'ananya@houseofmeera.com', TRUE, 'ACTIVE'),
    ('a1000001-0001-4001-8001-000000000002', 'VST-USR-100011', '+919876543211', TRUE, 'priya@example.com', FALSE, 'ACTIVE'),
    ('a1000001-0001-4001-8001-000000000004', 'VST-USR-100013', '+919876543213', TRUE, 'vikram@atelier-noir.com', TRUE, 'ACTIVE'),
    ('a1000001-0001-4001-8001-000000000005', 'VST-USR-100014', '+919876543214', TRUE, 'sanaya@studio-rhea.com', TRUE, 'ACTIVE');

INSERT INTO user_profile (user_id, first_name, last_name, display_name, preferences) VALUES
    ('a1000001-0001-4001-8001-000000000001', 'Ananya', 'Sharma', 'Ananya S.',
     '{"size":"S","occasions":["wedding","party"]}'::jsonb),
    ('a1000001-0001-4001-8001-000000000002', 'Priya', 'Kapoor', 'Priya K.', '{}'::jsonb),
    ('a1000001-0001-4001-8001-000000000004', 'Vikram', 'Desai', 'Vikram D.', '{}'::jsonb),
    ('a1000001-0001-4001-8001-000000000005', 'Sanaya', 'Iyer', 'Sanaya I.', '{}'::jsonb);

INSERT INTO user_role (user_id, role_id) VALUES
    ('a1000001-0001-4001-8001-000000000001', 1),
    ('a1000001-0001-4001-8001-000000000001', 2),
    ('a1000001-0001-4001-8001-000000000002', 1),
    ('a1000001-0001-4001-8001-000000000004', 1),
    ('a1000001-0001-4001-8001-000000000004', 2),
    ('a1000001-0001-4001-8001-000000000005', 1),
    ('a1000001-0001-4001-8001-000000000005', 2);

INSERT INTO address (id, user_id, label, line1, line2, city, state, pincode, phone, is_default) VALUES
    ('a8000001-0001-4001-8001-000000000001', 'a1000001-0001-4001-8001-000000000001',
     'Home', '14 Altamount Road', 'Flat 3B', 'Mumbai', 'Maharashtra', '400026', '+919876543210', TRUE),
    ('a8000001-0001-4001-8001-000000000003', 'a1000001-0001-4001-8001-000000000002',
     'Home', '22 Pali Hill', NULL, 'Mumbai', 'Maharashtra', '400058', '+919876543211', TRUE);

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

-- Link all catalog products to their seller profiles (by brand).
UPDATE product SET seller_profile_id = 'a2000001-0001-4001-8001-000000000001'
WHERE id IN (
    '44444444-4444-7444-8444-444444444401',
    '44444444-4444-7444-8444-444444444405'
);
UPDATE product SET seller_profile_id = 'a2000001-0001-4001-8001-000000000002'
WHERE id IN (
    '44444444-4444-7444-8444-444444444402',
    '44444444-4444-7444-8444-444444444404'
);
UPDATE product SET seller_profile_id = 'a2000001-0001-4001-8001-000000000003'
WHERE id = '44444444-4444-7444-8444-444444444403';

INSERT INTO wallet (id, seller_profile_id, available_balance, pending_balance, total_earned, total_withdrawn) VALUES
    ('a6000001-0001-4001-8001-000000000001', 'a2000001-0001-4001-8001-000000000001', 8500, 1200, 9700, 0),
    ('a6000001-0001-4001-8001-000000000002', 'a2000001-0001-4001-8001-000000000002', 4200, 0, 4200, 0),
    ('a6000001-0001-4001-8001-000000000003', 'a2000001-0001-4001-8001-000000000003', 1500, 0, 1500, 0);

SELECT setval('user_code_seq', GREATEST(
    (SELECT last_value FROM user_code_seq),
    100015
));
