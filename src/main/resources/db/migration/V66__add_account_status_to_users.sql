-- Add account_status column
ALTER TABLE accounts
ADD COLUMN account_status VARCHAR(20) DEFAULT 'ACTIVE';

-- Update existing records (optional but safe)
UPDATE accounts
SET account_status = 'ACTIVE'
WHERE account_status IS NULL;

-- Make it NOT NULL
ALTER TABLE accounts
ALTER COLUMN account_status SET NOT NULL;