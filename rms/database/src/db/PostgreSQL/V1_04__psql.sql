CREATE TABLE "builtin_repo_item" (
  id SERIAL,
  repo_id varchar(50) NOT NULL,
  file_path varchar(2000),
  file_path_display varchar(2000),
  file_path_search varchar(2000),
  last_modified timestamp WITH TIME ZONE NOT NULL,
  is_dir boolean NOT NULL,
  size_in_bytes bigint,
  PRIMARY KEY (id),
  CONSTRAINT u_file_path UNIQUE (repo_id, file_path),
  CONSTRAINT fk_built_in_repo FOREIGN KEY (repo_id) REFERENCES repository(id) ON DELETE CASCADE
);
