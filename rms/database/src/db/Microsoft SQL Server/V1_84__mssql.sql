CREATE TABLE rms.enterprise_space_item
(
   id NVARCHAR(36) NOT NULL,
   duid NVARCHAR(36),
   uploader_id INT NOT NULL,
   tenant_id NVARCHAR(36) NOT NULL,
   permissions INT,
   file_path NVARCHAR(2000),
   file_path_display NVARCHAR(2000),
   file_path_search NVARCHAR(255),
   creation_time DATETIME2(6) NOT NULL,
   expiration DATETIME2(6),
   last_modified DATETIME2(6) NOT NULL,
   last_modified_user_id INT,
   is_dir SMALLINT DEFAULT 0 NOT NULL,
   size_in_bytes BIGINT,
   file_parent_path NVARCHAR(2000),
   CONSTRAINT ENTERPRISE_SPACE_ITEM_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_ENTERPRISESPACE_TENANT FOREIGN KEY(tenant_id)
   REFERENCES rms.tenant(id) ON DELETE CASCADE,
   CONSTRAINT FK_ENTERPRISESPACE_USER FOREIGN KEY(uploader_id)
   REFERENCES rms.users(id) ON DELETE NO ACTION,
   CONSTRAINT FK_ENTERPRISESPACE_MODIFIER FOREIGN KEY(last_modified_user_id)
   REFERENCES rms.users(id) ON DELETE NO ACTION,
   CONSTRAINT U_FILE_PATH_ENTERPRISE UNIQUE(tenant_id,file_path)
);

ALTER TABLE rms.tenant ADD ews_size_used BIGINT NOT NULL DEFAULT 0;