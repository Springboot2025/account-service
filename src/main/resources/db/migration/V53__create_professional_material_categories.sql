-- =========================================================
-- V53__create_professional_material_categories.sql
-- =========================================================

CREATE TABLE professional_material_categories (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL UNIQUE,
    display_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- ---------------------------------------------------------
-- Seed default categories (from UI screenshots)
-- ---------------------------------------------------------
INSERT INTO professional_material_categories (uuid, name, display_order)
VALUES
    (gen_random_uuid(), 'Reports from Doctor', 1),
    (gen_random_uuid(), 'Psychologist Report', 2),
    (gen_random_uuid(), 'Psychiatrist Report', 3),
    (gen_random_uuid(), 'Legal Research', 4),
    (gen_random_uuid(), 'Other Report Material', 5);

-- ---------------------------------------------------------
-- Trigger to auto-update updated_at
-- ---------------------------------------------------------
CREATE TRIGGER trg_set_updated_at_professional_material_categories
BEFORE UPDATE ON professional_material_categories
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_timestamp();
