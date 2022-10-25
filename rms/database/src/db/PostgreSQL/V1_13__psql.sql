ALTER TABLE offline_file DROP CONSTRAINT fk_offline_file;
ALTER TABLE offline_file ADD CONSTRAINT fk_offline_file FOREIGN KEY (repository_id) REFERENCES repository(id) ON DELETE CASCADE;
ALTER TABLE favorite_file DROP CONSTRAINT fk_favorite_file;
ALTER TABLE favorite_file ADD CONSTRAINT fk_favorite_file FOREIGN KEY (repository_id) REFERENCES repository(id) ON DELETE CASCADE;