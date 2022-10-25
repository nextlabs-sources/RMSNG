ALTER TABLE membership ALTER COLUMN project_id DROP NOT NULL;
ALTER TABLE membership ALTER COLUMN tenant_id DROP NOT NULL;
update membership set type = 1 from project  where project.tenant_id = membership.tenant_id;
update membership set type = 0 from tenant  where tenant.id = membership.tenant_id and tenant.dns_name is not null;

ALTER TABLE tenant_classification RENAME TO classification;
ALTER TABLE classification ADD COLUMN type SMALLINT NOT NULL DEFAULT 0;
UPDATE classification
  SET type = CASE
              WHEN tenant.parent_id IS NOT NULL THEN 1
              ELSE 0
             END
FROM tenant WHERE tenant.id = classification.tenant_id;
ALTER TABLE classification ALTER COLUMN type DROP DEFAULT;
ALTER TABLE classification ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE classification ADD COLUMN project_id int;
UPDATE classification SET project_id = project.id FROM project WHERE project.tenant_id = classification.tenant_id;
ALTER TABLE classification ADD CONSTRAINT fk_clf_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE;
ALTER TABLE classification DROP CONSTRAINT fk_tenant_clf_tenant;
ALTER TABLE classification ADD CONSTRAINT fk_clf_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id) ON DELETE CASCADE;
ALTER TABLE classification DROP CONSTRAINT u_tenant_clf_name;
ALTER TABLE classification ADD CONSTRAINT u_clf_name UNIQUE (tenant_id, project_id, name);

ALTER TABLE policy_component ADD COLUMN type SMALLINT NOT NULL DEFAULT 0;
UPDATE policy_component
  SET type = CASE
              WHEN tenant.parent_id IS NOT NULL THEN 1
              ELSE 0
             END
FROM tenant WHERE tenant.id = policy_component.tenant_id;
ALTER TABLE policy_component ALTER COLUMN type DROP DEFAULT;
ALTER TABLE policy_component ADD COLUMN project_id int;
UPDATE policy_component
  SET project_id = project.id
FROM project WHERE project.tenant_id = policy_component.tenant_id;
ALTER TABLE policy_component ADD CONSTRAINT fk_policy_component_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE;
ALTER TABLE policy_component ALTER COLUMN tenant_id DROP NOT NULL;

ALTER TABLE nxl_metadata RENAME COLUMN tenant_id TO token_group_name;
ALTER TABLE nxl_metadata ALTER COLUMN token_group_name TYPE varchar(250);

ALTER TABLE key_store_entry RENAME COLUMN tenant_name TO token_group_name;

ALTER TABLE tenant ADD COLUMN keystore_id varchar(36);
UPDATE tenant SET keystore_id = key_store_entry.id FROM key_store_entry WHERE key_store_entry.token_group_name = tenant.name;
ALTER TABLE tenant ALTER COLUMN keystore_id SET NOT NULL;
ALTER TABLE tenant ADD CONSTRAINT fk_tenant_keystore FOREIGN KEY (keystore_id) REFERENCES key_store_entry (id) ON DELETE CASCADE;

ALTER TABLE project DROP COLUMN keystore;
ALTER TABLE project ADD COLUMN keystore_id varchar(36);
ALTER TABLE project ADD COLUMN alias_project_name varchar(50);
UPDATE project SET keystore_id = key_store_entry.id FROM key_store_entry, tenant WHERE key_store_entry.token_group_name = tenant.name AND tenant.id = project.tenant_id;
ALTER TABLE project ALTER COLUMN keystore_id SET NOT NULL;
UPDATE project SET alias_project_name=tenant.name FROM tenant WHERE project.tenant_id = tenant.id;
ALTER TABLE project RENAME COLUMN tenant_id TO parent_tenant_id;
ALTER TABLE project DROP CONSTRAINT u_project_name;
UPDATE project SET parent_tenant_id =
  CASE
    WHEN tenant.parent_id IS NULL THEN tenant.id
    ELSE tenant.parent_id
  END
FROM tenant WHERE tenant.id = project.parent_tenant_id;
ALTER TABLE project ADD CONSTRAINT fk_project_keystore FOREIGN KEY (keystore_id) REFERENCES key_store_entry (id) ON DELETE CASCADE;