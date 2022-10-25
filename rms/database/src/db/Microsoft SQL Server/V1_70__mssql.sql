CREATE TABLE rms.policy_component (
	id BIGINT IDENTITY NOT NULL,
	tenant_id NVARCHAR(36) NOT NULL,
	policy_id BIGINT NOT NULL,
	component_type INT NOT NULL,
	component_json text NOT NULL,
	PRIMARY KEY(id),
	CONSTRAINT fk_policy_component_tenant FOREIGN KEY(tenant_id) REFERENCES rms.tenant(id) ON DELETE CASCADE
);