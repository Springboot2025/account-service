-- ============================================
-- Table: case_types
-- ============================================
CREATE TABLE IF NOT EXISTS case_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

-- Seed initial types
INSERT INTO case_types (name, description)
VALUES
    ('Mentions', 'Minor matters / mentions'),
    ('Contested Matters', 'Contested legal matters'),
    ('Legal Matters', 'General legal matters')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- Alter: cases table - add case_type_id
-- ============================================
ALTER TABLE cases
    ADD COLUMN IF NOT EXISTS case_type_id BIGINT;

ALTER TABLE cases
    ADD CONSTRAINT IF NOT EXISTS fk_cases_case_type
    FOREIGN KEY (case_type_id) REFERENCES case_types(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_cases_case_type_id ON cases (case_type_id);
