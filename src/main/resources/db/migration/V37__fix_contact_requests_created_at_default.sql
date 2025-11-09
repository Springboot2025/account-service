-- Ensure created_at has a default value going forward
ALTER TABLE contact_requests
ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

-- Fix existing rows where created_at is NULL
UPDATE contact_requests
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;
