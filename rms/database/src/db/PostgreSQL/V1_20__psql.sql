CREATE TABLE project_invitation (
  id BIGSERIAL,
  project_id int NOT NULL,
  inviter_id int NOT NULL,
  invitee_email varchar(150) NOT NULL,
  invite_time timestamp WITH TIME ZONE NOT NULL,
  expire_time timestamp WITH TIME ZONE NOT NULL,
  action_time timestamp WITH TIME ZONE,
  status smallint NOT NULL DEFAULT 0,
  comment varchar(250),
  PRIMARY KEY (id),
  CONSTRAINT fk_pi_user FOREIGN KEY (inviter_id) REFERENCES "user"(id) ON DELETE CASCADE,
  CONSTRAINT fk_pi_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
);
