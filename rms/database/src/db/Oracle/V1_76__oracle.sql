ALTER TABLE project_space_item ADD last_modified_user_id NUMBER(10,0);
UPDATE project_space_item SET last_modified_user_id = user_id;
ALTER TABLE project_space_item ADD CONSTRAINT fk_projectspace_last_user FOREIGN KEY (last_modified_user_id) REFERENCES users(id);
ALTER TABLE policy_component ADD policy_model_type NUMBER(5,0) DEFAULT 0 NOT NULL;
ALTER TABLE policy_component ADD status NUMBER(5,0) DEFAULT 0 NOT NULL;
