-- =========================================================
-- V51__create_documents_template_center.sql
-- =========================================================

-- =========================================================
-- 1. System-defined document categories
-- =========================================================
CREATE TABLE document_categories (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    key VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- =========================================================
-- 2. Seed default categories (from UI screenshots)
-- =========================================================
INSERT INTO document_categories (uuid, key, display_name, display_order)
VALUES
    (gen_random_uuid(), 'CASE_RELATED', 'Case-Related Documents', 1),
    (gen_random_uuid(), 'FINANCIAL', 'Financial & Billing Documents', 2),
    (gen_random_uuid(), 'CLIENT', 'Client & Consultation Documents', 3),
    (gen_random_uuid(), 'LEGAL_TEMPLATES', 'Legal Templates & Forms', 4);

-- =========================================================
-- 3. Lawyer-created subheadings
-- =========================================================
CREATE TABLE lawyer_document_subheadings (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    lawyer_uuid UUID NOT NULL,
    category_id BIGINT NOT NULL
        REFERENCES document_categories(id),
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- =========================================================
-- 4. Documents Template Center (actual files/templates)
-- =========================================================
CREATE TABLE documents_template_center (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    lawyer_uuid UUID NOT NULL,
    subheading_id BIGINT NOT NULL
        REFERENCES lawyer_document_subheadings(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- =========================================================
-- 5. updated_at trigger function (shared pattern)
-- =========================================================
CREATE OR REPLACE FUNCTION set_updated_at_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =========================================================
-- 6. Triggers
-- =========================================================
CREATE TRIGGER trg_set_updated_at_document_categories
BEFORE UPDATE ON document_categories
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_timestamp();

CREATE TRIGGER trg_set_updated_at_lawyer_document_subheadings
BEFORE UPDATE ON lawyer_document_subheadings
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_timestamp();

CREATE TRIGGER trg_set_updated_at_documents_template_center
BEFORE UPDATE ON documents_template_center
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_timestamp();
