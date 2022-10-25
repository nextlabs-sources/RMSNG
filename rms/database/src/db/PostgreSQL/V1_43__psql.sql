DROP TABLE offline_file;
TRUNCATE TABLE favorite_file;
ALTER TABLE favorite_file DROP COLUMN parent_file_id;
ALTER TABLE favorite_file ADD parent_file_id_hash varchar(32) NOT NULL;
ALTER TABLE favorite_file ADD file_path_id_hash varchar(32) NOT NULL;
CREATE INDEX idx_file_path_id_hash ON favorite_file (repository_id, file_path_id_hash, status DESC);
CREATE INDEX idx_parent_file_id_hash ON favorite_file (repository_id, parent_file_id_hash, status DESC);
CREATE INDEX idx_status_last_modified ON favorite_file (repository_id, status, last_modified);