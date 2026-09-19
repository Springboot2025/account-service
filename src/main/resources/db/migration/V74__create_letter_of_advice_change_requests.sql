-- A client can ask the lawyer for changes to a Letter of Advice they were sent
-- but haven't signed yet. Several requests can accumulate per document; the
-- lawyer marks each one read (read_at) so the unread count in their header
-- only reflects requests they haven't dealt with.
CREATE TABLE letter_of_advice_change_requests (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    document_uuid UUID NOT NULL REFERENCES letter_of_advice_documents(uuid) ON DELETE CASCADE,
    client_uuid UUID NOT NULL REFERENCES accounts(uuid),
    category VARCHAR(40) NOT NULL,
    message TEXT NOT NULL,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_loa_change_requests_document ON letter_of_advice_change_requests(document_uuid);
CREATE INDEX idx_loa_change_requests_unread
    ON letter_of_advice_change_requests(document_uuid)
    WHERE read_at IS NULL;
