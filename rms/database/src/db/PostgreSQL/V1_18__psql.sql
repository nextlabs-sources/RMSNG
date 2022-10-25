ALTER TABLE builtin_repo_item ADD COLUMN duid varchar(36);
ALTER TABLE builtin_repo_item ADD COLUMN is_deleted boolean;
ALTER TABLE shared_nxl RENAME TO all_nxl;
ALTER TABLE all_nxl ADD COLUMN is_shared boolean;