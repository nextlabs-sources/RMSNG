CREATE TABLE tenant (
  id varchar(36) NOT NULL,
  name varchar(250),
  admin varchar(150),
  security_mode int NOT NULL,
  dns_name varchar(150),
  display_name varchar(150),
  login_icon bytea,
  preference varchar(2000) NOT NULL,
  creation_time timestamp WITH TIME ZONE NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT u_tenant_name UNIQUE (name)
);
CREATE TABLE "user" (
  id SERIAL,
  display_name varchar(150),
  email varchar(150),
  ticket bytea,
  ttl bigint NOT NULL DEFAULT 0,
  attempt int NOT NULL DEFAULT 0,
  preferences varchar(2000),
  creation_time timestamp WITH TIME ZONE NOT NULL,
  status smallint NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);
CREATE TABLE "group" (
  id SERIAL,
  name varchar(50) NOT NULL,
  tenant_id varchar(36) NOT NULL,
  external_id varchar(50),
  type int NOT NULL DEFAULT 0,
  keystore oid,
  display_name varchar(150) NOT NULL,
  description varchar(250),
  owner varchar(250),
  creation_time timestamp WITH TIME ZONE NOT NULL,
  last_modified timestamp WITH TIME ZONE NOT NULL,
  preferences varchar(2000),
  status smallint NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT u_group_name UNIQUE (name, tenant_id),
  CONSTRAINT fk_group_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);
CREATE TABLE membership (
  name varchar(150) NOT NULL,
  group_id int NOT NULL,
  tenant_id varchar(36) NOT NULL,
  user_id int NOT NULL,
  external_id varchar(50),
  type int NOT NULL DEFAULT 0,
  keystore oid,
  preferences varchar(2000),
  creation_time timestamp WITH TIME ZONE NOT NULL,
  last_modified timestamp WITH TIME ZONE NOT NULL,
  status smallint NOT NULL DEFAULT 0,
  PRIMARY KEY (name),
  CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
  CONSTRAINT fk_membership_group FOREIGN KEY (group_id) REFERENCES "group"(id) ON DELETE CASCADE,
  CONSTRAINT fk_membership_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);
CREATE TABLE login_account (
  id SERIAL,
  login_name varchar(150) NOT NULL,
  type int NOT NULL DEFAULT 0,
  password bytea,
  email varchar(150),
  user_id int NOT NULL,
  attempt int NOT NULL DEFAULT 0,
  otp bytea,
  creation_time timestamp WITH TIME ZONE NOT NULL,
  last_attempt timestamp WITH TIME ZONE,
  last_login timestamp WITH TIME ZONE,
  status smallint NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT u_login_account UNIQUE (login_name, type),
  CONSTRAINT fk_user_login_account FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);
CREATE TABLE storage_provider (
  id varchar(36) NOT NULL,
  tenant_id varchar(36) NOT NULL,
  name varchar(150),
  type int NOT NULL,
  attributes varchar(2000),
  creation_time timestamp WITH TIME ZONE NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_storage_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);
CREATE TABLE repository (
  id varchar(36) NOT NULL,
  provider_id varchar(36) NOT NULL,
  user_id int NOT NULL,
  name varchar(150),
  shared int NOT NULL,
  account_name varchar(250),
  account_id varchar(250),
  token varchar(512),
  rmc_token varchar(512),
  preference varchar(2000),
  creation_time timestamp WITH TIME ZONE NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_repository_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
  CONSTRAINT fk_repository_provider FOREIGN KEY (provider_id) REFERENCES storage_provider(id) ON DELETE CASCADE,
  CONSTRAINT u_repository_account UNIQUE (user_id, account_id)
);
CREATE TABLE black_list (
  duid varchar(36) NOT NULL,
  user_id int NOT NULL,
  creation_time timestamp WITH TIME ZONE NOT NULL,
  expiration timestamp WITH TIME ZONE NOT NULL,
  PRIMARY KEY (duid),
  CONSTRAINT fk_black_list_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);
CREATE TABLE shared_nxl (
  duid varchar(36) NOT NULL,
  user_id int NOT NULL,
  permissions int NOT NULL,
  metadata varchar(4000),
  file_name varchar(255),
  display_name varchar(255),
  creation_time timestamp WITH TIME ZONE NOT NULL,
  expiration timestamp WITH TIME ZONE NOT NULL,
  last_modified timestamp WITH TIME ZONE NOT NULL,
  status smallint NOT NULL DEFAULT 0,
  PRIMARY KEY (duid),
  CONSTRAINT fk_shared_nxl_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);
CREATE TABLE sharing_transaction (
  id varchar(36) NOT NULL,
  duid varchar(36) NOT NULL,
  user_id int NOT NULL,
  repository_id varchar(36),
  file_path_id varchar(1000),
  file_path varchar(1000),
  device_id varchar(32),
  device_type int NOT NULL,
  creation_time timestamp WITH TIME ZONE NOT NULL,
  status smallint NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_sharing_repository FOREIGN KEY (repository_id) REFERENCES repository(id) ON DELETE SET NULL,
  CONSTRAINT fk_sharing_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);
CREATE TABLE sharing_recipient (
  duid varchar(36) NOT NULL,
  email varchar(150) NOT NULL,
  transaction_id varchar(36) NOT NULL,
  last_modified timestamp WITH TIME ZONE NOT NULL,
  status smallint NOT NULL DEFAULT 0,
  PRIMARY KEY (duid, email),
  CONSTRAINT fk_recipient_transaction FOREIGN KEY (transaction_id) REFERENCES sharing_transaction(id) ON DELETE CASCADE
);
CREATE TABLE client (
  client_id varchar(32) NOT NULL,
  device_id varchar(32) NOT NULL,
  device_type int NOT NULL DEFAULT 0,
  manufacturer varchar(50),
  model varchar(32),
  os_version varchar(64),
  app_name varchar(150),
  app_version varchar(20),
  push_token varchar(64),
  status int NOT NULL DEFAULT 0,
  creation_date timestamp WITH TIME ZONE NOT NULL,
  last_modified timestamp WITH TIME ZONE NOT NULL,
  notes varchar(50),
  PRIMARY KEY (client_id)
);
CREATE TABLE activity_log (
  id varchar(36) NOT NULL,
  duid varchar(36) NOT NULL,
  owner varchar(150) NOT NULL,
  user_id int NOT NULL,
  operation int NOT NULL,
  device_id varchar(32),
  device_type int NOT NULL,
  repository_id varchar(36),
  file_path_id varchar(1000),
  file_name varchar(255),
  file_path varchar(1000),
  app_name varchar(150),
  app_path varchar(512),
  app_publisher varchar(150),
  access_time timestamp WITH TIME ZONE NOT NULL,
  access_result int NOT NULL,
  activity_data varchar(2000),
  PRIMARY KEY (id),
  CONSTRAINT fk_activity_log_repository FOREIGN KEY (repository_id) REFERENCES repository(id) ON DELETE SET NULL,
  CONSTRAINT fk_activity_log_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);
CREATE INDEX idx_duid_access_time ON activity_log (duid, access_time DESC);
CREATE INDEX idx_duid_access_result ON activity_log (duid, access_result);
