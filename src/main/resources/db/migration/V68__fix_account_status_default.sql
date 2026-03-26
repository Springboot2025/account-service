-- 1. Set default value (safe for new inserts)
ALTER TABLE accounts
ALTER COLUMN account_status SET DEFAULT 'ACTIVE';

-- 2. Backfill existing null values
UPDATE accounts
SET account_status = 'ACTIVE'
WHERE account_status IS NULL;

-- 3. Enforce NOT NULL constraint (if not already enforced)
ALTER TABLE accounts
ALTER COLUMN account_status SET NOT NULL;