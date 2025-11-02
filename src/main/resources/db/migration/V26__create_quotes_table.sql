-- V20251102__create_quotes_table.sql
CREATE TABLE IF NOT EXISTS quotes (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    lawyer_uuid UUID NOT NULL,      -- lawyer receiving the quote request
    client_uuid UUID NOT NULL,      -- client requesting the quote

    title VARCHAR(255) NULL,        -- e.g., "Contract Review"
    description TEXT NULL,          -- case or matter details
    expected_amount NUMERIC(12,2) NULL,  -- client's expected budget
    quoted_amount NUMERIC(12,2) NULL,    -- lawyer's quoted fee
    currency VARCHAR(8) DEFAULT 'AUD',

    status VARCHAR(50) NOT NULL DEFAULT 'REQUESTED'
        CHECK (status IN (
            'REQUESTED',
            'PENDING',
            'SENT',
            'ACCEPTED',
            'REJECTED',
            'CANCELLED'
        )),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT fk_quotes_lawyer_uuid
        FOREIGN KEY (lawyer_uuid) REFERENCES accounts (uuid) ON DELETE CASCADE,

    CONSTRAINT fk_quotes_client_uuid
        FOREIGN KEY (client_uuid) REFERENCES accounts (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_quotes_lawyer_uuid ON quotes(lawyer_uuid);
CREATE INDEX idx_quotes_client_uuid ON quotes(client_uuid);
CREATE INDEX idx_quotes_status ON quotes(status);
