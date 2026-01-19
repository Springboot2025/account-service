INSERT INTO case_status (name, description)
VALUES ('Urgent', 'Case requires urgent attention')
ON CONFLICT (name) DO NOTHING;
