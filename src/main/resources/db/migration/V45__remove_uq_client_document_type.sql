-- Drop unique constraint on (client_uuid, document_type, deleted_at)
ALTER TABLE client_documents
    DROP CONSTRAINT IF EXISTS uq_client_document_type;
