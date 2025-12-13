-- ============================================
-- Add quote_uuid to cases
-- ============================================

ALTER TABLE cases
ADD COLUMN quote_uuid UUID;

-- Foreign key constraint
ALTER TABLE cases
ADD CONSTRAINT fk_cases_quote
FOREIGN KEY (quote_uuid)
REFERENCES quotes (uuid)
ON DELETE RESTRICT;

-- Index for joins
CREATE INDEX IF NOT EXISTS idx_cases_quote_uuid
ON cases (quote_uuid);
