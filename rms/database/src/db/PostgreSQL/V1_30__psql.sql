ALTER TABLE customer_account ADD COLUMN tenant_id varchar(36) NOT NULL;
ALTER TABLE customer_account ADD CONSTRAINT fk_customer_account_tenant FOREIGN KEY (tenant_id) REFERENCES "tenant"(id);
ALTER TABLE project ADD COLUMN customer_account_id bigint;
ALTER TABLE project ADD CONSTRAINT fk_project_customer_account FOREIGN KEY (customer_account_id) REFERENCES "customer_account"(id);