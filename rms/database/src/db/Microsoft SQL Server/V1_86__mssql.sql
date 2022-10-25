ALTER TABLE rms.enterprise_space_item DROP CONSTRAINT U_FILE_PATH_ENTERPRISE;
ALTER TABLE rms.enterprise_space_item ALTER COLUMN file_path NVARCHAR(2000) COLLATE Latin1_General_CS_AS;
ALTER TABLE rms.enterprise_space_item ADD CONSTRAINT U_FILE_PATH_ENTERPRISE UNIQUE (tenant_id,file_path);
GO

ALTER TABLE rms.builtin_repo_item DROP CONSTRAINT U_FILE_PATH;
ALTER TABLE rms.builtin_repo_item ALTER COLUMN file_path NVARCHAR(2000) COLLATE Latin1_General_CS_AS;
ALTER TABLE rms.builtin_repo_item ADD CONSTRAINT U_FILE_PATH UNIQUE (repo_id,file_path);
GO

ALTER TABLE rms.project_space_item DROP CONSTRAINT U_FILE_PATH_PROJECT;
ALTER TABLE rms.project_space_item ALTER COLUMN file_path NVARCHAR(2000) COLLATE Latin1_General_CS_AS;
ALTER TABLE rms.project_space_item ADD CONSTRAINT U_FILE_PATH_PROJECT UNIQUE (project_id,file_path);
GO