CREATE TABLE document_content(document_id bigint PRIMARY KEY REFERENCES document_record(id) ON DELETE CASCADE, content bytea NOT NULL);
INSERT INTO document_content(document_id,content) SELECT id,content FROM document_record WHERE content IS NOT NULL;
ALTER TABLE document_record DROP COLUMN content;
