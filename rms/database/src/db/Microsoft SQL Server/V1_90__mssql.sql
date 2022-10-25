CREATE TABLE rms.external_repository_nxl
(
   duid NVARCHAR(36)  NOT NULL,
   user_id INT  NOT NULL,
   repository_id NVARCHAR(36) NOT NULL,
   permissions INT  NOT NULL,
   file_path NVARCHAR(2000) NOT NULL,
   file_name NVARCHAR(255) NOT NULL,
   display_name NVARCHAR(255) NOT NULL,
   creation_time DATETIME2(6) NOT NULL,
   last_modified DATETIME2(6) NOT NULL,
   status INT  DEFAULT 0  NOT NULL,
   is_shared SMALLINT  NOT NULL,
   owner NVARCHAR(150),
   CONSTRAINT EXTNL_REPO_NXL_PKEY PRIMARY KEY(duid),
   CONSTRAINT FK_EXTNL_REPO_NXL_USER FOREIGN KEY(user_id)
   REFERENCES rms.users(id),
   CONSTRAINT FK_EXTNL_REPO_ID FOREIGN KEY(repository_id)
   REFERENCES rms.repository(id)  ON DELETE CASCADE
);

CREATE INDEX IDX_EXTNL_REPO_USER_ID
ON rms.external_repository_nxl
(user_id);

CREATE INDEX IDX_EXTNL_REPOSITORY_ID
ON rms.external_repository_nxl
(repository_id);

