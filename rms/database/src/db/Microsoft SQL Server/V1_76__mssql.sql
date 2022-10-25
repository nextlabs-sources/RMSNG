ALTER TABLE rms.project_space_item ADD last_modified_user_id INT;
go
UPDATE rms.project_space_item SET last_modified_user_id = user_id;
ALTER TABLE rms.project_space_item ALTER COLUMN last_modified_user_id INT NOT NULL;
ALTER TABLE rms.project_space_item ADD CONSTRAINT fk_projectspace_last_user FOREIGN KEY (last_modified_user_id) REFERENCES rms.users(id);
ALTER TABLE rms.policy_component ADD policy_model_type SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE rms.policy_component ADD status SMALLINT NOT NULL DEFAULT 0;

ALTER TABLE rms.tag
   DROP CONSTRAINT fk_tags_tenant;

ALTER TABLE rms.tag
   ADD CONSTRAINT fk_tags_tenant
   FOREIGN KEY (tenant_id) REFERENCES rms.tenant(id) ON DELETE CASCADE;
