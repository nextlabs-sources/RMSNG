CREATE TABLE api_user_cert (
  id varchar(36) NOT NULL,
  api_user_id int NOT NULL,
  data oid not null,
  cert_alias varchar(250),
  creation_time timestamp WITH TIME ZONE NOT NULL,
  last_modified timestamp WITH TIME ZONE NOT NULL,
  PRIMARY KEY(id),
  CONSTRAINT fk_api_user_id FOREIGN KEY (api_user_id) REFERENCES "users"(id) ON DELETE CASCADE,
  CONSTRAINT u_api_user_cert_alias UNIQUE (api_user_id, cert_alias)
);