CREATE TABLE policy_bundle
(
  id serial NOT NULL,
  tenant_id character varying(36) NOT NULL,
  policy_bundles text NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_policy_bundle_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);