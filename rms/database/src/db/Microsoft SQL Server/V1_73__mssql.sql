CREATE TABLE rms.tenant_classification
(
   id NVARCHAR(36) NOT NULL,
   name NVARCHAR(50) NOT NULL,
   tenant_id NVARCHAR(36) NOT NULL,
   is_multi_sel SMALLINT NOT NULL,
   is_mandatory SMALLINT NOT NULL,   
   labels NVARCHAR(1200) NOT NULL,
   parent_id NVARCHAR(36),
   order_id INT NOT NULL,
   CONSTRAINT TENANT_CLF_PKEY PRIMARY KEY(id),
   CONSTRAINT U_TENANT_CLF_NAME UNIQUE(tenant_id, name),
   CONSTRAINT FK_TENANT_CLF_TENANT FOREIGN KEY (tenant_id) 
   REFERENCES rms.tenant(id) ON DELETE CASCADE
);

DROP TABLE rms.project_classification;

ALTER TABLE rms.tenant ADD configuration_modified DATETIME2(6) NOT NULL DEFAULT getutcdate();