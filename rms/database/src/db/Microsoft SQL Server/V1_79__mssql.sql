ALTER TABLE rms.membership ALTER COLUMN project_id INT NULL;
ALTER TABLE rms.membership ALTER COLUMN tenant_id NVARCHAR(36) NULL;
go
update rms.membership set type = 1 from rms.project  where project.tenant_id = membership.tenant_id;
update rms.membership set type = 0 from rms.tenant  where tenant.id = membership.tenant_id and tenant.dns_name is not null;

EXEC sp_rename 'rms.tenant_classification', 'classification';
go
ALTER TABLE rms.classification ADD type SMALLINT;
go

UPDATE rms.classification
  SET type = CASE
                WHEN rms.tenant.parent_id IS NOT NULL THEN 1
                ELSE 0
               END
FROM rms.tenant WHERE rms.tenant.id = rms.classification.tenant_id;
ALTER TABLE rms.classification ALTER COLUMN type SMALLINT NOT NULL;
ALTER TABLE rms.classification ALTER COLUMN tenant_id nvarchar(36) NULL;
ALTER TABLE rms.classification ADD project_id INT;
GO

UPDATE rms.classification SET project_id = rms.project.id FROM rms.project WHERE rms.project.tenant_id = rms.classification.tenant_id;

ALTER TABLE rms.classification ADD CONSTRAINT fk_clf_project FOREIGN KEY (project_id) REFERENCES rms.project (id);
ALTER TABLE rms.classification DROP CONSTRAINT fk_tenant_clf_tenant;
ALTER TABLE rms.classification ADD CONSTRAINT fk_clf_tenant FOREIGN KEY (tenant_id) REFERENCES rms.tenant (id);
ALTER TABLE rms.classification DROP CONSTRAINT u_tenant_clf_name;
ALTER TABLE rms.classification ADD CONSTRAINT u_clf_name UNIQUE (tenant_id, project_id, name);
go


ALTER TABLE rms.policy_component ADD type SMALLINT;
go
UPDATE rms.policy_component
  SET type = CASE
              WHEN rms.tenant.parent_id IS NOT NULL THEN 1
              ELSE 0
             END
FROM rms.tenant WHERE tenant.id = rms.policy_component.tenant_id;
ALTER TABLE rms.policy_component ALTER COLUMN type SMALLINT NOT NULL;
ALTER TABLE rms.policy_component ADD project_id INT;
go
UPDATE rms.policy_component 
  SET project_id = rms.project.id 
  FROM rms.project WHERE rms.project.tenant_id = rms.policy_component.tenant_id;
ALTER TABLE rms.policy_component ADD CONSTRAINT fk_policy_component_project FOREIGN KEY (project_id) REFERENCES rms.project (id);
ALTER TABLE rms.policy_component ALTER COLUMN tenant_id nvarchar(36) NULL;
go

EXEC sp_rename 'rms.nxl_metadata.tenant_id', 'token_group_name', 'COLUMN';
EXEC sp_rename 'rms.key_store_entry.tenant_name', 'token_group_name', 'COLUMN';
go

ALTER TABLE rms.nxl_metadata ALTER COLUMN token_group_name NVARCHAR(250);
ALTER TABLE rms.tenant ADD keystore_id NVARCHAR(36);
go
update rms.nxl_metadata
set  token_group_name=tenant.name
from rms.tenant where nxl_metadata.token_group_name=tenant.id;

UPDATE rms.tenant SET keystore_id = rms.key_store_entry.id FROM rms.key_store_entry WHERE rms.key_store_entry.token_group_name = rms.tenant.name;
ALTER TABLE rms.tenant ALTER COLUMN keystore_id NVARCHAR(36) NOT NULL;
ALTER TABLE rms.tenant ADD CONSTRAINT fk_tenant_keystore FOREIGN KEY (keystore_id) REFERENCES rms.key_store_entry (id) ON DELETE CASCADE;
go

ALTER TABLE rms.project DROP COLUMN keystore;
ALTER TABLE rms.project ADD keystore_id NVARCHAR(36);
ALTER TABLE rms.project ADD alias_project_name varchar(50);
go

UPDATE rms.project SET alias_project_name=tenant.name FROM rms.tenant WHERE project.tenant_id = tenant.id;
UPDATE rms.project SET keystore_id = rms.key_store_entry.id 
FROM rms.key_store_entry, rms.tenant 
WHERE rms.key_store_entry.token_group_name = rms.tenant.name AND rms.tenant.id = rms.project.tenant_id;

EXEC sp_rename 'rms.project.tenant_id', 'parent_tenant_id', 'COLUMN';
ALTER TABLE rms.project ALTER COLUMN keystore_id NVARCHAR(36) NOT NULL;
GO

UPDATE rms.project set parent_tenant_id =
  CASE
    WHEN rms.tenant.parent_id IS NULL THEN rms.tenant.id
    ELSE rms.tenant.parent_id
  END
FROM rms.tenant WHERE rms.tenant.id = rms.project.parent_tenant_id; 
ALTER TABLE rms.project ADD CONSTRAINT fk_project_keystore FOREIGN KEY (keystore_id) REFERENCES rms.key_store_entry (id) ON DELETE NO ACTION;
