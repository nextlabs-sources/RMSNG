ALTER TABLE project ADD COLUMN default_invitation_msg character varying(250);
ALTER TABLE project_invitation ADD COLUMN invitation_msg character varying(250);