INSERT INTO case_status (name, description)
VALUES
    ('Won', 'Case has been Won'),
    ('Lost', 'Case has been Lost')
ON CONFLICT (name) DO NOTHING;