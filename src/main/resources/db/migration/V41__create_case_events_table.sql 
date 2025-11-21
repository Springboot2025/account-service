CREATE TABLE IF NOT EXISTS case_events (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    case_uuid UUID NOT NULL, -- FK to cases.uuid
    event_date DATE NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    title VARCHAR(255),
    details TEXT NOT NULL,
    status VARCHAR(128) NOT NULL,
    related_date DATE,
    user_name VARCHAR(255) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

-- Index for fast retrieval of history for a case
CREATE INDEX IF NOT EXISTS idx_case_events_case_uuid
    ON case_events (case_uuid);

-- Index for ordering events by date
CREATE INDEX IF NOT EXISTS idx_case_events_event_date
    ON case_events (event_date DESC);
