CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NULL,
    mobile VARCHAR(20),
    date_of_birth DATE,
    terms BOOLEAN DEFAULT FALSE NOT NULL,
    newsletter BOOLEAN DEFAULT FALSE NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE NOT NULL,
    is_active BOOLEAN DEFAULT FALSE NOT NULL,
    verification_token UUID,
    organization VARCHAR(250),
    experience VARCHAR(25),
    office_address TEXT,
    team_size VARCHAR(50),
    languages VARCHAR(250),
    address TEXT,
    address_details JSONB NULL,
    contact_information JSONB NULL,
    emergency_contact JSONB NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMP
);

-- Index for faster login lookup
CREATE INDEX idx_accounts_email ON accounts (email);
