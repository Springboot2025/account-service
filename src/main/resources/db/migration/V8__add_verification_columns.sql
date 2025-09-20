-- V8__add_verification_columns.sql

ALTER TABLE accounts
    ADD COLUMN is_verified BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN is_active BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN verification_token UUID;

ALTER TABLE accounts ALTER COLUMN password DROP NOT NULL;
