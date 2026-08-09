ALTER TABLE user_profile
    ADD COLUMN gender VARCHAR(20);

UPDATE user_profile
SET gender = 'PREFER_NOT_TO_SAY'
WHERE gender IS NULL;

ALTER TABLE user_profile
    ALTER COLUMN gender SET NOT NULL;
