ALTER TABLE accounts
    ADD COLUMN date_of_birth DATE,
    ADD COLUMN terms BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN newsletter BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN organization VARCHAR(250),
    ADD COLUMN experience VARCHAR(25),
    ADD COLUMN office_address TEXT,
    ADD COLUMN team_size VARCHAR(50),
    ADD COLUMN languages VARCHAR(250);
