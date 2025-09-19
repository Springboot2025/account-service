-- 1. Create client_documents table
CREATE TABLE client_documents (
    id BIGSERIAL PRIMARY KEY,
    client_uuid UUID NOT NULL REFERENCES accounts(uuid) ON DELETE CASCADE,
    lawyer_uuid UUID NULL, -- one doc belongs to one lawyer
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL, -- pdf, docx, xlsx etc
    file_url TEXT NOT NULL, -- GCS public/private URL
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,

    -- Optional uniqueness: same client cannot upload duplicate filename for same lawyer
    CONSTRAINT uq_client_lawyer_file UNIQUE (client_uuid, lawyer_uuid, file_name, deleted_at)
);

-- 2. Trigger function to auto-update updated_at
CREATE OR REPLACE FUNCTION set_updated_at_client_docs()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Trigger for updated_at
CREATE TRIGGER trg_set_updated_at_client_docs
BEFORE UPDATE ON client_documents
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_client_docs();
