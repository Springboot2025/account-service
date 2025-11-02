CREATE TABLE IF NOT EXISTS user_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    user_uuid UUID NOT NULL,                     -- who submitted the feedback
    rating NUMERIC(2,1) NOT NULL CHECK (rating >= 1.0 AND rating <= 5.0),
    review TEXT NOT NULL,
    is_public BOOLEAN DEFAULT TRUE,              -- visible on public testimonial pages

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT fk_feedback_user FOREIGN KEY (user_uuid) REFERENCES accounts (uuid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_feedbacks_user_uuid ON user_feedbacks(user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_feedbacks_rating ON user_feedbacks(rating);
CREATE INDEX IF NOT EXISTS idx_user_feedbacks_is_public ON user_feedbacks(is_public);
