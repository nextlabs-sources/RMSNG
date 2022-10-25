CREATE TABLE tenant_classification
(
    id character varying(36) NOT NULL,
    name character varying(60) NOT NULL,
    tenant_id character varying(36) NOT NULL,
    is_multi_sel boolean,
    is_mandatory boolean,
    labels character varying(1200) NOT NULL,
    parent_id character varying(36),
    order_id integer NOT NULL,
    PRIMARY KEY (id ),
    CONSTRAINT u_tenant_clf_name UNIQUE (tenant_id , name ),
    CONSTRAINT fk_tenant_clf_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id) ON DELETE CASCADE
);

DROP TABLE project_classification;

ALTER TABLE tenant ADD COLUMN configuration_modified timestamp WITH TIME ZONE NOT NULL DEFAULT now();