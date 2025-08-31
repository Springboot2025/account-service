CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    email VARCHAR(150) UNIQUE NOT NULL,
    mobile VARCHAR(20),
    address TEXT,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMP
);

-- Index for faster login lookup
CREATE INDEX idx_accounts_email ON accounts (email);
