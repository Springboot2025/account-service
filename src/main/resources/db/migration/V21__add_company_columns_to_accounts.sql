-- Add company support to accounts table
ALTER TABLE accounts
ADD COLUMN IF NOT EXISTS is_company BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS company_uuid UUID NULL;

-- Create index for quick lookups by company
CREATE INDEX IF NOT EXISTS idx_accounts_company_uuid ON accounts (company_uuid);

-- Optional: add FK constraint to enforce referential integrity with companies table
ALTER TABLE accounts
ADD CONSTRAINT fk_accounts_company
FOREIGN KEY (company_uuid) REFERENCES companies (uuid)
ON DELETE SET NULL;
