DROP INDEX idx_parent_file_path_hash;
DROP INDEX idx_repo_id;
CREATE INDEX idx_repo_id_parent_hash ON builtin_repo_item (repo_id, parent_file_path_hash);