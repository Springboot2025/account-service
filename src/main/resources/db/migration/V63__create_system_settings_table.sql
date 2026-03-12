-- V63__create_system_settings_table.sql

CREATE TABLE system_settings (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() NOT NULL UNIQUE,

    settings JSONB NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMP
);

-- Index for quick lookup
CREATE INDEX idx_system_settings_uuid ON system_settings (uuid);