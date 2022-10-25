CREATE TABLE enterprise_space_item (
  id varchar(36) NOT NULL,
  duid varchar(36),
  uploader_id int NOT NULL,
  tenant_id varchar(36) NOT NULL,
  permissions int,
  file_path varchar(2000) NOT NULL,
  file_path_display varchar(2000) NOT NULL,
  file_path_search varchar(255) NOT NULL,
  creation_time timestamp WITH TIME ZONE NOT NULL,
  is_dir boolean NOT NULL DEFAULT FALSE,
  size_in_bytes bigint,
  file_parent_path varchar(2000),
  expiration timestamp WITH TIME ZONE,
  last_modified timestamp WITH TIME ZONE NOT NULL,
  last_modified_user_id int,
  PRIMARY KEY(id),
  CONSTRAINT fk_ews_tenant FOREIGN KEY (tenant_id) REFERENCES "tenant"(id) ON DELETE CASCADE,
  CONSTRAINT fk_ews_user FOREIGN KEY (uploader_id) REFERENCES "users"(id) ON DELETE NO ACTION,
  CONSTRAINT fk_ews_last_user FOREIGN KEY (last_modified_user_id) REFERENCES "users"(id) ON DELETE NO ACTION,
  CONSTRAINT u_file_path_ews UNIQUE (tenant_id , file_path)
);

ALTER TABLE tenant ADD COLUMN ews_size_used bigint DEFAULT 0;