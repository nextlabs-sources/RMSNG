ALTER TABLE project_space_item ADD COLUMN status smallint NOT NULL DEFAULT 0;
ALTER TABLE sharing_transaction ADD COLUMN src_project_id integer;