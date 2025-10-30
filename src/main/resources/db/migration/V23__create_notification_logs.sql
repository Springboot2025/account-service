CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGSERIAL PRIMARY KEY,
    user_uuid UUID NOT NULL,
    device_id VARCHAR(255),
    fcm_token TEXT,
    message_id TEXT,
    title VARCHAR(255),
    body TEXT,
    payload JSONB,               -- optional full payload
    status VARCHAR(50) NOT NULL, -- e.g. SENT, FAILED
    error_message TEXT,
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- helpful indexes
CREATE INDEX IF NOT EXISTS idx_notification_logs_user ON notification_logs (user_uuid);
CREATE INDEX IF NOT EXISTS idx_notification_logs_message_id ON notification_logs (message_id);