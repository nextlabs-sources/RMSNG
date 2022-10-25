UPDATE builtin_repo_item SET is_deleted = false WHERE is_deleted IS NULL; 
ALTER TABLE builtin_repo_item ALTER COLUMN is_deleted SET NOT NULL;
