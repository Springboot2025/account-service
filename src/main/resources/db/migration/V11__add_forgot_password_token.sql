-- Flyway migration: Add forgot_password_token column for password reset flow

-- 1. Add column (nullable so existing rows are unaffected)
ALTER TABLE accounts
ADD COLUMN forgot_password_token UUID NULL;

-- 2. Create index for faster lookups by token
CREATE INDEX idx_accounts_forgot_password_token
    ON accounts(forgot_password_token);
