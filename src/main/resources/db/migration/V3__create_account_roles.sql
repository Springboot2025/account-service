CREATE TABLE account_roles (
    account_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id, role_id),
    CONSTRAINT fk_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- Optional indexes (helpful for queries like "find roles by account" or "find accounts by role")
CREATE INDEX idx_account_roles_account_id ON account_roles (account_id);
CREATE INDEX idx_account_roles_role_id ON account_roles (role_id);
