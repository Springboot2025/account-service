CREATE TABLE IF NOT EXISTS client_appointments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    client_uuid UUID NOT NULL,
    lawyer_uuid UUID NOT NULL,

    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    meeting_type VARCHAR(50) DEFAULT 'Video Call',
    notes TEXT,

    status VARCHAR(30) DEFAULT 'PENDING',
    rescheduled_from UUID NULL, -- track previous appointment if rescheduled

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT fk_client_appointment_client FOREIGN KEY (client_uuid)
        REFERENCES accounts (uuid) ON DELETE CASCADE,

    CONSTRAINT fk_client_appointment_lawyer FOREIGN KEY (lawyer_uuid)
        REFERENCES accounts (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_client_appointments_client_uuid ON client_appointments(client_uuid);
CREATE INDEX idx_client_appointments_lawyer_uuid ON client_appointments(lawyer_uuid);
CREATE INDEX idx_client_appointments_status ON client_appointments(status);
CREATE INDEX idx_client_appointments_date_time ON client_appointments(appointment_date, appointment_time);
