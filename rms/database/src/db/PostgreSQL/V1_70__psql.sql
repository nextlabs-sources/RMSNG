CREATE TABLE policy_component (
	id BIGSERIAL,
	tenant_id varchar(36) NOT NULL,
	policy_id bigint NOT NULL,
	component_type int NOT NULL,
	component_json text NOT NULL,
	PRIMARY KEY(id),
	CONSTRAINT fk_policy_component_tenant FOREIGN KEY(tenant_id) REFERENCES "tenant"(id) ON DELETE CASCADE
);