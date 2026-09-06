CREATE TABLE letter_of_advice_templates (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    lawyer_uuid UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_loa_templates_lawyer_uuid ON letter_of_advice_templates(lawyer_uuid);

CREATE TABLE letter_of_advice_documents (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    lawyer_uuid UUID NOT NULL,
    client_uuid UUID NOT NULL,
    quote_uuid UUID NOT NULL,
    template_uuid UUID,
    title VARCHAR(255),
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    lawyer_signature TEXT,
    lawyer_signed_at TIMESTAMP,
    client_signature TEXT,
    client_signed_at TIMESTAMP,
    sent_to_client_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_loa_documents_lawyer_uuid ON letter_of_advice_documents(lawyer_uuid);
CREATE INDEX idx_loa_documents_client_uuid ON letter_of_advice_documents(client_uuid);
CREATE INDEX idx_loa_documents_quote_uuid ON letter_of_advice_documents(quote_uuid);

-- One active (non-deleted) Letter of Advice per quote/matter
CREATE UNIQUE INDEX uq_loa_documents_quote_uuid_active
    ON letter_of_advice_documents(quote_uuid)
    WHERE deleted_at IS NULL;
