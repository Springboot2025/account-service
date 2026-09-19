-- A case can now accumulate several Letters of Advice over its life. Starting
-- a new one used to soft-delete the previous letter (deleted_at), which hid it
-- from the lawyer's history and from the client's Letters list. Instead the
-- old letter is marked superseded and stays visible, with its sent/signed
-- dates intact. Only one letter per case is "current" (not deleted and not
-- superseded), so the uniqueness rule moves to that.
ALTER TABLE letter_of_advice_documents ADD COLUMN superseded_at TIMESTAMP;

DROP INDEX IF EXISTS uq_loa_documents_case_uuid_active;

CREATE UNIQUE INDEX uq_loa_documents_case_uuid_current
    ON letter_of_advice_documents(case_uuid)
    WHERE deleted_at IS NULL AND superseded_at IS NULL;
