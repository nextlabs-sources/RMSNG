ALTER TABLE builtin_repo_item ALTER COLUMN file_path_search TYPE character varying(255);
ALTER TABLE project_space_item ALTER COLUMN file_path_search TYPE character varying(255);


ALTER TABLE builtin_repo_item ALTER COLUMN file_path_search SET NOT NULL;
ALTER TABLE builtin_repo_item ALTER COLUMN file_path_display SET NOT NULL;
ALTER TABLE builtin_repo_item ALTER COLUMN file_path SET NOT NULL;

ALTER TABLE project_space_item ALTER COLUMN file_path_search SET NOT NULL;
ALTER TABLE project_space_item ALTER COLUMN file_path_display SET NOT NULL;
ALTER TABLE project_space_item ALTER COLUMN file_path SET NOT NULL;