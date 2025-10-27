-- ===================================================================
-- Create table: lawyer_ratings
-- Clients can rate and review Lawyers (1.0–5.0)
-- ===================================================================

CREATE TABLE IF NOT EXISTS lawyer_ratings (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    lawyer_uuid UUID NOT NULL,        -- The lawyer being rated
    client_uuid UUID NOT NULL,        -- The client giving the rating
    rating NUMERIC(2,1) NOT NULL CHECK (rating >= 1.0 AND rating <= 5.0),
    review TEXT,                      -- Optional review/comment

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT fk_lawyer FOREIGN KEY (lawyer_uuid) REFERENCES accounts (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_client FOREIGN KEY (client_uuid) REFERENCES accounts (uuid) ON DELETE CASCADE
);

-- Indexes for quick lookup
CREATE INDEX IF NOT EXISTS idx_lawyer_ratings_lawyer_uuid ON lawyer_ratings (lawyer_uuid);
CREATE INDEX IF NOT EXISTS idx_lawyer_ratings_client_uuid ON lawyer_ratings (client_uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uniq_lawyer_client_rating
    ON lawyer_ratings (lawyer_uuid, client_uuid)
    WHERE deleted_at IS NULL;  -- Prevent multiple active ratings from same client to same lawyer
