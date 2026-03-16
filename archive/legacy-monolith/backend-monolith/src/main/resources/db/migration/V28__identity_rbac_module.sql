ALTER TABLE users
    ADD COLUMN IF NOT EXISTS username VARCHAR(100),
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

UPDATE users
SET username = LOWER(email)
WHERE username IS NULL;

UPDATE users
SET status = CASE
    WHEN enabled THEN 'ACTIVE'
    ELSE 'DISABLED'
END
WHERE status IS NULL OR status = '';

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username_lower ON users (LOWER(username));
