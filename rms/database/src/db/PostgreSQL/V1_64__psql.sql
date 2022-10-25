DROP TABLE identity_provider;
CREATE TABLE identity_provider (
    id SERIAL NOT NULL,
    tenant_id varchar(36) NOT NULL,
    type int NOT NULL,
    attributes varchar,
    user_attribute_map varchar(1200),
    PRIMARY KEY (id),
    CONSTRAINT fk_identity_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);
CREATE TABLE tenant_user_attribute (
    id varchar(36) NOT NULL,
    tenant_id varchar(36) NOT NULL,
    name varchar(50) NOT NULL,
    is_custom boolean,
    is_selected boolean,
    PRIMARY KEY (id),
    CONSTRAINT u_tenant_attrib_name UNIQUE(tenant_id, name),
    CONSTRAINT fk_tenant_user_attrib_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);