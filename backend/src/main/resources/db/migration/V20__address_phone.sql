ALTER TABLE address
    ADD COLUMN phone VARCHAR(15);

UPDATE address a
SET phone = u.phone
FROM "user" u
WHERE a.user_id = u.id
  AND a.phone IS NULL;

ALTER TABLE address
    ALTER COLUMN phone SET NOT NULL;
