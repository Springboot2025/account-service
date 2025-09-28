ALTER TABLE accounts
    ADD COLUMN address_details JSONB NULL,
    ADD COLUMN contact_information JSONB NULL,
    ADD COLUMN emergency_contact JSONB NULL;
