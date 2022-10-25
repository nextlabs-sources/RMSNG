CREATE TABLE feedback (
  id varchar(36) NOT NULL,
  type varchar(250) NOT NULL, 
  summary varchar(100) NOT NULL, -- size from UI
  description varchar(2000) NOT NULL, -- size from UI
  client_id varchar(32) NOT NULL,
  device_id varchar(32),
  device_type int NOT NULL,
  user_id int NOT NULL,
  creation_time timestamp WITH TIME ZONE NOT NULL,
  PRIMARY KEY (id)
);