ALTER TABLE rms.customer_account DROP CONSTRAINT fk_customer_account_tenant;
ALTER TABLE rms.customer_account DROP COLUMN tenant_id;