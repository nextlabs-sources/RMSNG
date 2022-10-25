CREATE TABLE project_space_item (
  id varchar(36) NOT NULL,
  duid varchar(36) NOT NULL,
  user_id int NOT NULL,
  project_id int NOT NULL,
  permissions int,
  file_path varchar(2000),
  file_path_display varchar(2000),
  file_path_search varchar(2000),
  creation_time timestamp WITH TIME ZONE NOT NULL,
  expiration timestamp WITH TIME ZONE,
  last_modified timestamp WITH TIME ZONE NOT NULL,
  PRIMARY KEY(id),
  CONSTRAINT fk_projectspace_project FOREIGN KEY (project_id) REFERENCES "project"(id) ON DELETE CASCADE,
  CONSTRAINT fk_projectspace_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);
