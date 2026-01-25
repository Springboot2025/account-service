ALTER TABLE accounts
ADD COLUMN consultation_rates JSONB NULL;

ALTER TABLE accounts
DROP COLUMN IF EXISTS languages;

ALTER TABLE accounts
ADD COLUMN languages JSONB NULL;
