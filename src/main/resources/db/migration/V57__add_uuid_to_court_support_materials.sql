-- 1. Add column (nullable first — safe for existing records)
ALTER TABLE court_support_materials
ADD COLUMN uuid UUID;

-- 2. Backfill UUID values for existing rows
UPDATE court_support_materials
SET uuid = gen_random_uuid()
WHERE uuid IS NULL;

-- 3. Set NOT NULL constraint (after backfill)
ALTER TABLE court_support_materials
ALTER COLUMN uuid SET NOT NULL;

-- 4. Add unique constraint
ALTER TABLE court_support_materials
ADD CONSTRAINT uk_court_support_materials_uuid UNIQUE (uuid);
