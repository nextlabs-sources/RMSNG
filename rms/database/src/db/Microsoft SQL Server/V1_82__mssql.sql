ALTER TABLE rms.policy_component ALTER COLUMN  component_json text NULL;
go
ALTER TABLE rms.project DROP CONSTRAINT u_project_name;
go
