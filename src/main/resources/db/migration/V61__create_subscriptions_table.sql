-- V61__create_subscriptions_table.sql

CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() NOT NULL UNIQUE,

    plan_name VARCHAR(150) NOT NULL,
    description TEXT,

    monthly_price NUMERIC(10,2) NOT NULL,
    annual_price NUMERIC(10,2) NOT NULL,

    recommended BOOLEAN DEFAULT FALSE,

    features JSONB NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMP
);

-- Index for faster lookup by uuid
CREATE INDEX idx_subscriptions_uuid ON subscriptions (uuid);