-- ============================================
-- Table: case_status
-- ============================================
CREATE TABLE IF NOT EXISTS case_status (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

-- Seed initial statuses
INSERT INTO case_status (name, description)
VALUES
    ('New', 'Newly created case'),
    ('Active', 'Ongoing case'),
    ('Pending', 'Case is pending'),
    ('Closed', 'Case closed'),
    ('Deleted', 'Case deleted')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- Table: cases
-- ============================================
CREATE TABLE IF NOT EXISTS cases (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    case_number VARCHAR(128) UNIQUE NOT NULL,
    listing VARCHAR(255),
    court_date DATE,
    available_trust_funds NUMERIC(19,2) DEFAULT 0,
    follow_up VARCHAR(255),

    status_id BIGINT NOT NULL DEFAULT 1, -- references 'New'

    client_uuid UUID NOT NULL,
    lawyer_uuid UUID NOT NULL,

    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    deleted_at TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT fk_cases_client FOREIGN KEY (client_uuid)
        REFERENCES accounts (uuid) ON DELETE RESTRICT,

    CONSTRAINT fk_cases_lawyer FOREIGN KEY (lawyer_uuid)
        REFERENCES accounts (uuid) ON DELETE RESTRICT,

    CONSTRAINT fk_cases_status FOREIGN KEY (status_id)
        REFERENCES case_status (id) ON DELETE RESTRICT
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_cases_client_uuid ON cases (client_uuid);
CREATE INDEX IF NOT EXISTS idx_cases_lawyer_uuid ON cases (lawyer_uuid);
CREATE INDEX IF NOT EXISTS idx_cases_status_id ON cases (status_id);
