-- V10__create_court_support_materials.sql

CREATE TABLE court_support_materials (
    id BIGSERIAL PRIMARY KEY,
    client_uuid UUID NOT NULL REFERENCES accounts(uuid) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_url TEXT NOT NULL,
    description JSONB, -- ✅ store structured JSON
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,

    CONSTRAINT uq_client_file UNIQUE (client_uuid, file_name, deleted_at)
);

-- Trigger function
CREATE OR REPLACE FUNCTION set_updated_at_court_support_materials()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger
CREATE TRIGGER trg_set_updated_at_court_support_materials
BEFORE UPDATE ON court_support_materials
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_court_support_materials();
