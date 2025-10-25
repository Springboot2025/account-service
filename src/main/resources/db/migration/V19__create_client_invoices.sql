CREATE TABLE client_invoices (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    case_uuid UUID NOT NULL,
    lawyer_uuid UUID NOT NULL,

    trust_balance NUMERIC(12,2),
    amount_requested NUMERIC(12,2) NOT NULL,
    due_date DATE,
    last_activity VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT fk_client_invoices_case_uuid
        FOREIGN KEY (case_uuid) REFERENCES cases (uuid),

    CONSTRAINT fk_client_invoices_lawyer_uuid
        FOREIGN KEY (lawyer_uuid) REFERENCES accounts (uuid)
);

-- ---------------------------------------------------------------------
-- Indexes for performance
-- ---------------------------------------------------------------------
CREATE INDEX idx_client_invoices_case_uuid ON client_invoices(case_uuid);
CREATE INDEX idx_client_invoices_lawyer_uuid ON client_invoices(lawyer_uuid);
CREATE INDEX idx_client_invoices_due_date ON client_invoices(due_date);
CREATE INDEX idx_client_invoices_status ON client_invoices(status);

