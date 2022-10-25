CREATE TABLE identity_provider (
    id varchar(36) NOT NULL,
    tenant_id varchar(36) NOT NULL,
    type int NOT NULL,
    attributes varchar,
    PRIMARY KEY (id),
    CONSTRAINT fk_identity_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);