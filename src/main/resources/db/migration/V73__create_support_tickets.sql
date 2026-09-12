CREATE TABLE support_tickets (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_by_uuid UUID NOT NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    category VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    assigned_admin_uuid UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_ticket_created_by FOREIGN KEY (created_by_uuid) REFERENCES accounts (uuid),
    CONSTRAINT fk_ticket_assigned_admin FOREIGN KEY (assigned_admin_uuid) REFERENCES accounts (uuid)
);
CREATE INDEX idx_support_tickets_status ON support_tickets (status);
CREATE INDEX idx_support_tickets_created_by ON support_tickets (created_by_uuid);
CREATE INDEX idx_support_tickets_created_at ON support_tickets (created_at);

CREATE TABLE support_ticket_replies (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    ticket_uuid UUID NOT NULL,
    sender_uuid UUID NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reply_ticket FOREIGN KEY (ticket_uuid) REFERENCES support_tickets (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_reply_sender FOREIGN KEY (sender_uuid) REFERENCES accounts (uuid)
);
CREATE INDEX idx_support_ticket_replies_ticket ON support_ticket_replies (ticket_uuid);

CREATE TABLE support_ticket_attachments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    ticket_uuid UUID NOT NULL,
    reply_uuid UUID NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    file_type VARCHAR(100),
    uploaded_by_uuid UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachment_ticket FOREIGN KEY (ticket_uuid) REFERENCES support_tickets (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_attachment_reply FOREIGN KEY (reply_uuid) REFERENCES support_ticket_replies (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_attachment_uploaded_by FOREIGN KEY (uploaded_by_uuid) REFERENCES accounts (uuid)
);
CREATE INDEX idx_support_ticket_attachments_ticket ON support_ticket_attachments (ticket_uuid);
CREATE INDEX idx_support_ticket_attachments_reply ON support_ticket_attachments (reply_uuid);
