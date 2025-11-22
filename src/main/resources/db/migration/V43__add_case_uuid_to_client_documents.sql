ALTER TABLE client_documents
ADD COLUMN case_uuid UUID NULL;

ALTER TABLE client_documents
ADD CONSTRAINT fk_client_documents_case
FOREIGN KEY (case_uuid)
REFERENCES cases(uuid)
ON DELETE SET NULL;
