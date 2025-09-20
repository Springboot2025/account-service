-- V7__add_document_type_to_client_documents.sql
-- Adds document_type column and unique constraint for one document type per client

ALTER TABLE client_documents
ADD COLUMN document_type VARCHAR(100) NOT NULL DEFAULT 'general';

-- Drop old constraint if exists (optional, adjust name if different)
-- ALTER TABLE client_documents DROP CONSTRAINT IF EXISTS uq_client_lawyer_file;

-- Add a new uniqueness constraint: one document type per client
ALTER TABLE client_documents
ADD CONSTRAINT uq_client_document_type UNIQUE (client_uuid, document_type, deleted_at);
