ALTER TABLE project_space_item ALTER duid DROP NOT NULL, ADD COLUMN is_dir boolean NOT NULL DEFAULT FALSE, ADD COLUMN size_in_bytes bigint, ADD COLUMN file_parent_path character varying(2000);
