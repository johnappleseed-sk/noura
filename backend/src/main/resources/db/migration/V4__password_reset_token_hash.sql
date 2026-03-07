ALTER TABLE password_reset_tokens
    RENAME COLUMN token TO token_hash;

UPDATE password_reset_tokens
SET token_hash = encode(digest(token_hash, 'sha256'), 'hex')
WHERE token_hash IS NOT NULL
  AND token_hash !~ '^[0-9a-f]{64}$';

ALTER TABLE password_reset_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64);
