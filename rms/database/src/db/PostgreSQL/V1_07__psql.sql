CREATE TABLE resource_lock
(
  resource character varying(255) NOT NULL,
  last_updated timestamp with time zone NOT NULL,
  status smallint NOT NULL DEFAULT 0,
  PRIMARY KEY(resource)
);