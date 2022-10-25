ALTER TABLE project_space_item ADD COLUMN last_modified_user_id INTEGER;
UPDATE project_space_item SET last_modified_user_id = user_id;
ALTER TABLE project_space_item ALTER COLUMN last_modified_user_id SET NOT NULL;
ALTER TABLE project_space_item ADD CONSTRAINT fk_projectspace_last_user FOREIGN KEY (last_modified_user_id) REFERENCES "users"(id);
ALTER TABLE policy_component ADD COLUMN policy_model_type smallint NOT NULL DEFAULT 0;
ALTER TABLE policy_component ADD COLUMN status smallint NOT NULL DEFAULT 0;
