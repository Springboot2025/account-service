CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    actor_uuid UUID,
    actor_name VARCHAR(255),

    activity_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,

    case_uuid UUID,
    client_uuid UUID,
    lawyer_uuid UUID,
    reference_uuid UUID,

    metadata JSONB,

    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_activity_logs_timestamp ON activity_logs (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_activity_logs_lawyer_uuid ON activity_logs (lawyer_uuid);
CREATE INDEX IF NOT EXISTS idx_activity_logs_case_uuid ON activity_logs (case_uuid);
CREATE INDEX IF NOT EXISTS idx_activity_logs_client_uuid ON activity_logs (client_uuid);
CREATE INDEX IF NOT EXISTS idx_activity_logs_actor_uuid ON activity_logs (actor_uuid);
