-- V41__create_company_invites.sql
CREATE TABLE IF NOT EXISTS company_invites (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    company_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days'),
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP
);

-- index for lookups by token
CREATE INDEX IF NOT EXISTS idx_company_invites_token ON company_invites (token);
CREATE INDEX IF NOT EXISTS idx_company_invites_company_uuid ON company_invites (company_uuid);
