CREATE TABLE offline_file (
  id varchar(36) NOT NULL,
  repository_id varchar(36) NOT NULL,
  file_path_id varchar(1000) NOT NULL,
  file_path varchar(1000) NOT NULL,
  last_modified timestamp WITH TIME ZONE NOT NULL,
  status smallint NOT NULL DEFAULT 0,
  PRIMARY KEY(id),
  UNIQUE(repository_id, file_path_id, file_path),
  CONSTRAINT fk_offline_file FOREIGN KEY (repository_id) REFERENCES repository(id) ON DELETE SET NULL
);

CREATE TABLE favorite_file (
  id varchar(36) NOT NULL,
  repository_id varchar(36) NOT NULL,
  file_path_id varchar(1000) NOT NULL,
  file_path varchar(1000) NOT NULL,
  last_modified timestamp WITH TIME ZONE NOT NULL,
  status smallint NOT NULL DEFAULT 0,
  PRIMARY KEY(id),
  UNIQUE(repository_id, file_path_id, file_path),
  CONSTRAINT fk_favorite_file FOREIGN KEY (repository_id) REFERENCES repository(id) ON DELETE SET NULL
);