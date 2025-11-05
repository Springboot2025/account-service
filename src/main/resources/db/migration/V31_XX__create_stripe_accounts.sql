CREATE TABLE stripe_accounts (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    lawyer_uuid UUID NOT NULL,
    stripe_account_id VARCHAR(255) NOT NULL,
    charges_enabled BOOLEAN DEFAULT false,
    payouts_enabled BOOLEAN DEFAULT false,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stripe_accounts_lawyer
        FOREIGN KEY (lawyer_uuid) REFERENCES accounts(uuid)
);

CREATE UNIQUE INDEX idx_stripe_accounts_lawyer_uuid ON stripe_accounts(lawyer_uuid);
