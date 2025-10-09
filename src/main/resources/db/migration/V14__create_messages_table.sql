CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    sender_uuid UUID NOT NULL REFERENCES accounts(uuid) ON DELETE CASCADE,
    receiver_uuid UUID NOT NULL REFERENCES accounts(uuid) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by_sender BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by_receiver BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_messages_participants_created
    ON messages (sender_uuid, receiver_uuid, created_at DESC);

CREATE INDEX idx_messages_receiver_unread
    ON messages (receiver_uuid, is_read);
