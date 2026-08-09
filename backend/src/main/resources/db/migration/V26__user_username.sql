-- Add unique username to user profiles for password-based login

ALTER TABLE user_profile ADD COLUMN username VARCHAR(30);

CREATE UNIQUE INDEX uk_user_profile_username ON user_profile (LOWER(username))
    WHERE username IS NOT NULL;
