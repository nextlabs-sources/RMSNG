CREATE TABLE enterprise_space_item
(
   id VARCHAR2(36) NOT NULL,
   duid VARCHAR2(36),
   uploader_id NUMBER(10,0) NOT NULL,
   tenant_id VARCHAR2(36) NOT NULL,
   permissions NUMBER(10,0),
   file_path VARCHAR2(2000),
   file_path_display VARCHAR2(2000),
   file_path_search VARCHAR2(255),
   creation_time TIMESTAMP(6) NOT NULL,
   expiration TIMESTAMP(6),
   last_modified TIMESTAMP(6) NOT NULL,
   last_modified_user_id NUMBER(10,0),
   is_dir NUMBER(1,0)  DEFAULT 0 NOT NULL,
   size_in_bytes NUMBER(19,0),
   file_parent_path VARCHAR2(2000),
   CONSTRAINT enterprise_space_item_pkey PRIMARY KEY(id),
   CONSTRAINT fk_enterprisespace_tenant FOREIGN KEY(tenant_id)
   REFERENCES tenant(id)  ON DELETE CASCADE,
   CONSTRAINT fk_enterprisespace_uploader FOREIGN KEY(uploader_id)
   REFERENCES users(id),
   CONSTRAINT fk_enterprisespace_modifier FOREIGN KEY(last_modified_user_id)
   REFERENCES users(id),
   CONSTRAINT u_file_path_tenant UNIQUE(tenant_id,file_path)
);

ALTER TABLE tenant ADD ews_size_used NUMBER(19,0) DEFAULT 0;