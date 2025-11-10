CREATE TABLE disputes (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    organization VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    reference VARCHAR(255),
    incident_date DATE NOT NULL,
    type_of_dispute VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    resolution_requested TEXT NOT NULL,
    confirm_accuracy BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dispute_documents (
    id BIGSERIAL PRIMARY KEY,
    dispute_uuid UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dispute_document FOREIGN KEY (dispute_uuid)
        REFERENCES disputes (uuid)
        ON DELETE CASCADE
);
