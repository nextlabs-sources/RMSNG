TRUNCATE TABLE favorite_file;
ALTER TABLE favorite_file ADD COLUMN parent_file_id varchar(2000) NOT NULL;
ALTER TABLE favorite_file ADD COLUMN file_path_search varchar(255) NOT NULL;
ALTER TABLE favorite_file ADD COLUMN file_last_modified timestamp with time zone;
ALTER TABLE favorite_file ADD COLUMN file_size bigint;
