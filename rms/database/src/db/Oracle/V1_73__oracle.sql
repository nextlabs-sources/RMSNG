CREATE TABLE tenant_classification (
    id VARCHAR2(36) NOT NULL,
    name VARCHAR2(50) NOT NULL,
    tenant_id VARCHAR2(36) NOT NULL,
    is_multi_sel NUMBER(1,0) NOT NULL,
    is_mandatory NUMBER(1,0) NOT NULL,
    labels VARCHAR2(1200) NOT NULL,
	parent_id VARCHAR2(36),
	order_id NUMBER(10,0) NOT NULL,
    CONSTRAINT u_tenant_clf_name UNIQUE(tenant_id, name),
    CONSTRAINT tenant_clf_pkey PRIMARY KEY (id),
    CONSTRAINT fk_tenant_clf_tenant FOREIGN KEY (tenant_id)
    REFERENCES tenant(id) ON DELETE CASCADE
);

DROP TABLE project_classification;

ALTER TABLE tenant ADD configuration_modified TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL;