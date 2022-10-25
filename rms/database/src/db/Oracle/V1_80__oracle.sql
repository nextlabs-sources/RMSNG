ALTER TABLE customer_account DROP CONSTRAINT fk_customer_account_tenant;
ALTER TABLE customer_account DROP COLUMN tenant_id;