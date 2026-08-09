-- Business identifiers: product_code, user_code; rental_number rename; VST-* formats

ALTER TABLE product ADD COLUMN IF NOT EXISTS product_code VARCHAR(32);
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS user_code VARCHAR(32);

ALTER TABLE booking RENAME COLUMN booking_number TO rental_number;
ALTER TABLE booking ALTER COLUMN rental_number TYPE VARCHAR(32);
ALTER TABLE booking ALTER COLUMN order_number TYPE VARCHAR(32);

CREATE SEQUENCE IF NOT EXISTS product_code_seq START 100001;
CREATE SEQUENCE IF NOT EXISTS user_code_seq START 100001;
CREATE SEQUENCE IF NOT EXISTS order_number_seq START 1;
CREATE SEQUENCE IF NOT EXISTS rental_number_seq START 1;

-- Backfill product codes
WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn FROM product
)
UPDATE product p
SET product_code = 'VST-PROD-' || LPAD(n.rn::text, 6, '0')
FROM numbered n
WHERE p.id = n.id AND p.product_code IS NULL;

SELECT setval('product_code_seq', GREATEST(100001, COALESCE(
    (SELECT MAX(CAST(SUBSTRING(product_code FROM 10) AS BIGINT)) FROM product WHERE product_code LIKE 'VST-PROD-%'), 100000
) + 1));

-- Backfill user codes
WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn FROM "user"
)
UPDATE "user" u
SET user_code = 'VST-USR-' || LPAD(n.rn::text, 6, '0')
FROM numbered n
WHERE u.id = n.id AND u.user_code IS NULL;

SELECT setval('user_code_seq', GREATEST(100001, COALESCE(
    (SELECT MAX(CAST(SUBSTRING(user_code FROM 9) AS BIGINT)) FROM "user" WHERE user_code LIKE 'VST-USR-%'), 100000
) + 1));

-- Backfill rental and order numbers to VST-* format
WITH numbered AS (
    SELECT id,
           ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn,
           TO_CHAR(created_at AT TIME ZONE 'UTC', 'YYYYMMDD') AS dt
    FROM booking
)
UPDATE booking b
SET rental_number = 'VST-RNT-' || n.dt || '-' || LPAD(n.rn::text, 4, '0'),
    order_number = 'VST-ORD-' || n.dt || '-' || LPAD(n.rn::text, 4, '0')
FROM numbered n
WHERE b.id = n.id;

SELECT setval('rental_number_seq', GREATEST(1, (SELECT COUNT(*) FROM booking) + 1));
SELECT setval('order_number_seq', GREATEST(1, (SELECT COUNT(*) FROM booking) + 1));

ALTER TABLE product ALTER COLUMN product_code SET NOT NULL;
ALTER TABLE "user" ALTER COLUMN user_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_product_code ON product (product_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_user_code ON "user" (user_code);

DROP SEQUENCE IF EXISTS booking_number_seq;
