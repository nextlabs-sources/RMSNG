CREATE TABLE identity_provider (
    id VARCHAR2(36) NOT NULL,
    tenant_id VARCHAR2(36) NOT NULL,
    type NUMBER(10,0) NOT NULL,
    attributes CLOB,
    CONSTRAINT identity_provider_pkey PRIMARY KEY (id),
    CONSTRAINT fk_identity_tenant FOREIGN KEY (tenant_id)
	REFERENCES tenant(id) ON DELETE CASCADE
);