-- Business details collected on the "Firm Details" subscription screen.
-- Nullable: existing companies (created via registration, before this screen
-- existed) have none of this yet.
ALTER TABLE companies
    ADD COLUMN abn VARCHAR(20),
    ADD COLUMN business_phone VARCHAR(30),
    ADD COLUMN business_address VARCHAR(255),
    ADD COLUMN primary_contact_name VARCHAR(150),
    ADD COLUMN primary_contact_email VARCHAR(150);
