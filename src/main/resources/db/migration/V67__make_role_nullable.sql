-- Make role column nullable
ALTER TABLE disputes
ALTER COLUMN role DROP NOT NULL;