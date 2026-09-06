-- A matter accumulates many documents over its life (Letter of Advice being
-- just one of them) — the case is the durable anchor, not the one-time quote.
DROP INDEX IF EXISTS uq_loa_documents_quote_uuid_active;
DROP INDEX IF EXISTS idx_loa_documents_quote_uuid;

ALTER TABLE letter_of_advice_documents RENAME COLUMN quote_uuid TO case_uuid;

CREATE INDEX idx_loa_documents_case_uuid ON letter_of_advice_documents(case_uuid);

CREATE UNIQUE INDEX uq_loa_documents_case_uuid_active
    ON letter_of_advice_documents(case_uuid)
    WHERE deleted_at IS NULL;
