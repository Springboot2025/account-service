ALTER TABLE court_support_materials
    ADD COLUMN IF NOT EXISTS case_uuid UUID;

ALTER TABLE court_support_materials
    ADD CONSTRAINT fk_court_support_material_case
        FOREIGN KEY (case_uuid)
        REFERENCES cases (uuid)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_court_support_materials_case_uuid
    ON court_support_materials (case_uuid);
