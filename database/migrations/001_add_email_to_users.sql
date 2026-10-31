-- Run this migration on an existing java_chat database.
-- Back up your database before applying schema changes.

USE java_chat;

-- Step 1: Add email as nullable so existing rows remain valid.
ALTER TABLE users
    ADD COLUMN email VARCHAR(254) NULL AFTER username;

-- Step 2: Assign each existing account its real email address.
-- Replace the example values below with the actual email addresses.
-- Run one UPDATE per existing user, using the correct user_id.
-- UPDATE users SET email = 'person@example.com' WHERE user_id = 1;

-- Step 3: Confirm no rows have a missing email before continuing.
SELECT user_id, username
FROM users
WHERE email IS NULL OR TRIM(email) = '';

-- Step 4: Only after every existing account has a unique, valid email,
-- run these statements:
-- ALTER TABLE users MODIFY email VARCHAR(254) NOT NULL;
-- ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);
