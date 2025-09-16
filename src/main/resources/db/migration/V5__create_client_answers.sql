-- Create client_answers table
CREATE TABLE client_answers (
    id BIGSERIAL PRIMARY KEY,
    client_uuid UUID NOT NULL REFERENCES accounts(uuid) ON DELETE CASCADE,
    question_type VARCHAR(50) NOT NULL CHECK (question_type IN ('Core', 'Offence')),
    answers JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,

    CONSTRAINT uq_client_question UNIQUE (client_uuid, question_type)
);

-- JSONB index
CREATE INDEX idx_client_answers_answers_gin ON client_answers USING gin (answers);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_updated_at
BEFORE UPDATE ON client_answers
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
