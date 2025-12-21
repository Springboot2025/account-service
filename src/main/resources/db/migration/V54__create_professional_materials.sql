-- =========================================================
-- V54__create_professional_materials.sql
-- =========================================================

CREATE TABLE professional_materials (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,

    case_uuid UUID NOT NULL
        REFERENCES cases(uuid),

    category_id BIGINT NOT NULL
        REFERENCES professional_material_categories(id),

    follow_up TEXT NOT NULL,
    description TEXT,

    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_url TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- ---------------------------------------------------------
-- Indexes for performance
-- ---------------------------------------------------------
CREATE INDEX idx_professional_materials_case_uuid
    ON professional_materials(case_uuid);

CREATE INDEX idx_professional_materials_category_id
    ON professional_materials(category_id);

-- ---------------------------------------------------------
-- Trigger to auto-update updated_at
-- ---------------------------------------------------------
CREATE TRIGGER trg_set_updated_at_professional_materials
BEFORE UPDATE ON professional_materials
FOR EACH ROW
EXECUTE FUNCTION set_updated_at_timestamp();
