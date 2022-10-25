ALTER TABLE favorite_file DROP CONSTRAINT uk_fav_file_1;
ALTER TABLE favorite_file ADD CONSTRAINT uk_fav_file_1 UNIQUE (repository_id, file_path_id);
ALTER TABLE builtin_repo_item ALTER COLUMN repo_id TYPE character varying(36);