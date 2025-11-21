ALTER TABLE case_events
ADD CONSTRAINT fk_case_events_case
FOREIGN KEY (case_uuid)
REFERENCES cases (uuid)
ON DELETE RESTRICT;
