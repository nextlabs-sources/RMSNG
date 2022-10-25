ALTER TABLE project_invitation RENAME COLUMN comment to comments;
ALTER TABLE sharing_transaction RENAME COLUMN comment to comments;

ALTER TABLE favorite_file DROP CONSTRAINT IF EXISTS favorite_file_repository_id_file_path_id_file_path_key;
ALTER TABLE favorite_file ADD CONSTRAINT uk_fav_file_1 UNIQUE (repository_id, file_path_id, file_path);

ALTER TABLE resource_lock DROP CONSTRAINT resource_lock_pkey;
ALTER TABLE resource_lock RENAME COLUMN resource to id;
ALTER TABLE resource_lock ADD PRIMARY KEY (id);

ALTER TABLE "user" RENAME TO users;