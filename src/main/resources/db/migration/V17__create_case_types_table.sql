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

-- Add column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'cases' AND column_name = 'case_type_id'
    ) THEN
        ALTER TABLE cases ADD COLUMN case_type_id BIGINT;
    END IF;
END $$;

-- Add constraint safely (only if missing)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_cases_case_type'
          AND table_name = 'cases'
    ) THEN
        ALTER TABLE cases
            ADD CONSTRAINT fk_cases_case_type
            FOREIGN KEY (case_type_id)
            REFERENCES case_types(id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- Add index safely
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'cases' AND indexname = 'idx_cases_case_type_id'
    ) THEN
        CREATE INDEX idx_cases_case_type_id ON cases (case_type_id);
    END IF;
END $$;
