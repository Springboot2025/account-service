-- Ensure the uuid column is actually of type UUID
ALTER TABLE accounts
ALTER COLUMN uuid TYPE uuid USING uuid::uuid;

-- Make sure it has a default value using gen_random_uuid()
ALTER TABLE accounts
ALTER COLUMN uuid SET DEFAULT gen_random_uuid();

-- Ensure NOT NULL and UNIQUE constraints
ALTER TABLE accounts
ALTER COLUMN uuid SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_accounts_uuid ON accounts (uuid);
