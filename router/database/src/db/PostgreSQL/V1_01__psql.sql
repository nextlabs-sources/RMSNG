CREATE TABLE tenant (
  id varchar(36) NOT NULL,
  name varchar(250),
  otp bytea,
  hsk bytea,
  server varchar(150),
  display_name varchar(150),
  description varchar(2000),
  email varchar(250),
  creation_time timestamp WITH TIME ZONE NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT u_tenant_name UNIQUE (name)
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
CREATE TABLE crash (
    hash varchar(32) NOT NULL,
    client_id varchar(32),
    stacktrace varchar(9000),
    PRIMARY KEY (hash)
);
CREATE TABLE crash_log (
    id SERIAL,
    hash varchar(32) NOT NULL,
    log varchar(9000),
    creation_date timestamp WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);
