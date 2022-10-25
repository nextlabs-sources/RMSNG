DROP TABLE identity_provider;
CREATE TABLE identity_provider (
    id NUMBER(10,0) NOT NULL,
    tenant_id VARCHAR2(36) NOT NULL,
    type NUMBER(10,0) NOT NULL,
    attributes CLOB,
    user_attribute_map VARCHAR2(1200),
    CONSTRAINT identity_provider_pkey PRIMARY KEY (id),
    CONSTRAINT fk_identity_tenant FOREIGN KEY (tenant_id)
    REFERENCES tenant(id) ON DELETE CASCADE
);
CREATE SEQUENCE  "IDENTITY_PROVIDER_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;
CREATE TABLE tenant_user_attribute (
    id VARCHAR2(36) NOT NULL,
    tenant_id VARCHAR2(36) NOT NULL,
    name VARCHAR2(50) NOT NULL,
    is_custom NUMBER(1,0) NOT NULL,
    is_selected NUMBER(1,0) NOT NULL,
    CONSTRAINT u_tenant_attrib_name UNIQUE(tenant_id, name),
    CONSTRAINT tenant_user_attrib_pkey PRIMARY KEY (id),
    CONSTRAINT fk_tenant_user_attrib_tenant FOREIGN KEY (tenant_id)
    REFERENCES tenant(id) ON DELETE CASCADE
);