CREATE INDEX idx_repo_user_provider ON repository(user_id, provider_id);
CREATE INDEX idx_duid ON builtin_repo_item (duid);
CREATE INDEX idx_user_id ON all_nxl (user_id);