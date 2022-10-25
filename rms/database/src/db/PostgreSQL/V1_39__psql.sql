ALTER TABLE builtin_repo_item ADD COLUMN parent_file_path_hash character varying(32);

UPDATE builtin_repo_item SET parent_file_path_hash = md5(substring(file_path from 1 for (length(file_path) - position(reverse(file_path_search) in reverse(file_path)) - length(file_path_search) + 1)));

CREATE INDEX idx_parent_file_path_hash ON builtin_repo_item (parent_file_path_hash);

ALTER TABLE builtin_repo_item ALTER COLUMN parent_file_path_hash SET NOT NULL;