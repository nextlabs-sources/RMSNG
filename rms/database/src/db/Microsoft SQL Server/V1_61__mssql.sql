CREATE TABLE rms.identity_provider (
    id NVARCHAR(36) NOT NULL,
    tenant_id NVARCHAR(36) NOT NULL,
    type INT NOT NULL,
    attributes NVARCHAR,
    CONSTRAINT IDENTITY_PROVIDER_PKEY PRIMARY KEY (id),
    CONSTRAINT FK_IDENTITY_TENANT FOREIGN KEY (tenant_id) 
	REFERENCES rms.tenant(id) ON DELETE CASCADE
);