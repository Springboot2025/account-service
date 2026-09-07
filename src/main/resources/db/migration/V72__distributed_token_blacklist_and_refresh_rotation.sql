-- Replaces the in-memory access-token blacklist (which never worked once the
-- service ran more than one Cloud Run instance) with a DB-backed one, and
-- adds refresh-token rotation/reuse tracking (previously refresh tokens were
-- never invalidated at all -- neither on rotation nor on logout).

CREATE TABLE revoked_tokens (
    jti         VARCHAR(64) PRIMARY KEY,
    expires_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens (expires_at);

CREATE TABLE refresh_sessions (
    jti         VARCHAR(64) PRIMARY KEY,
    family_id   VARCHAR(64) NOT NULL,
    user_uuid   UUID NOT NULL,
    issued_at   TIMESTAMP NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_refresh_sessions_family_id ON refresh_sessions (family_id);
CREATE INDEX idx_refresh_sessions_user_uuid ON refresh_sessions (user_uuid);
CREATE INDEX idx_refresh_sessions_expires_at ON refresh_sessions (expires_at);
