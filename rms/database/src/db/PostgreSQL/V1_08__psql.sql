CREATE TABLE task_status
(
  resource character varying(255) NOT NULL,
  last_successful_update timestamp with time zone NOT NULL,
  last_failed_update timestamp with time zone NOT NULL,
  status smallint NOT NULL DEFAULT 0,
  PRIMARY KEY(resource)
);