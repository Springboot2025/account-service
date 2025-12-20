-- =========================================================
-- V52__create_shared_documents_table.sql
-- =========================================================

CREATE TABLE shared_documents (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE,

    lawyer_uuid UUID NOT NULL
        REFERENCES accounts(uuid),

    document_uuid UUID NOT NULL
        REFERENCES documents_template_center(uuid),

    client_uuid UUID NOT NULL
        REFERENCES accounts(uuid),

    case_uuid UUID NOT NULL
        REFERENCES cases(uuid),

    remarks TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- =========================================================
-- Indexes (performance)
-- =========================================================

CREATE INDEX idx_shared_documents_lawyer
    ON shared_documents (lawyer_uuid);

CREATE INDEX idx_shared_documents_document
    ON shared_documents (document_uuid);

CREATE INDEX idx_shared_documents_client
    ON shared_documents (client_uuid);

CREATE INDEX idx_shared_documents_case
    ON shared_documents (case_uuid);

-- =========================================================
-- Notes
-- =========================================================
-- 1. UUID is UNIQUE and API-safe
-- 2. All references enforced:
--    - lawyer_uuid  -> accounts(uuid)
--    - client_uuid  -> accounts(uuid)
--    - document_uuid -> documents_template_center(uuid)
--    - case_uuid -> cases(uuid)
-- 3. Duplicate prevention intentionally skipped (dev stage)
-- 4. Soft delete supported via deleted_at
-- =========================================================
