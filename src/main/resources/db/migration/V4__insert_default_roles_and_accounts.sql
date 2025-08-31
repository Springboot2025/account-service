-- Insert default roles
INSERT INTO roles (name, description)
VALUES
    ('SuperAdmin', 'Full system access'),
    ('Lawyer', 'Access to lawyer-specific features'),
    ('Client', 'Access to client-specific features')
ON CONFLICT (name) DO NOTHING;

-- Insert default accounts (with hashed passwords using bcrypt)
INSERT INTO accounts (first_name, last_name, gender, email, mobile, address, password)
VALUES
    ('Super', 'Admin', 'Other', 'superadmin@example.com', '1111111111', 'HQ', crypt('superadmin123', gen_salt('bf'))),
    ('Test', 'Lawyer', 'Male', 'lawyer@example.com', '2222222222', 'Law Office', crypt('lawyer123', gen_salt('bf'))),
    ('Test', 'Client', 'Female', 'client@example.com', '3333333333', 'Client Address', crypt('client123', gen_salt('bf')))
ON CONFLICT (email) DO NOTHING;

-- Assign roles to accounts
INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id FROM accounts a, roles r
WHERE a.email = 'superadmin@example.com' AND r.name = 'SuperAdmin'
ON CONFLICT DO NOTHING;

INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id FROM accounts a, roles r
WHERE a.email = 'lawyer@example.com' AND r.name = 'Lawyer'
ON CONFLICT DO NOTHING;

INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id FROM accounts a, roles r
WHERE a.email = 'client@example.com' AND r.name = 'Client'
ON CONFLICT DO NOTHING;
