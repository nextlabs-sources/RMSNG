ALTER TABLE membership MODIFY project_id NULL;
ALTER TABLE membership MODIFY tenant_id NULL;
merge into membership m using project p on ( p.tenant_id = m.tenant_id) when matched then update set type =1;
merge into membership m using tenant t on (t.id = m.tenant_id and t.dns_name is not null) when matched then update set type = 0;

RENAME tenant_classification TO classification;
ALTER TABLE classification ADD type NUMBER(1,0) DEFAULT 0 NOT NULL;
update CLASSIFICATION
   set type = (select case when t.parent_id is not null
                              then 1
                              else 0 end
                    from TENANT t
                   where t.ID = TENANT_ID); 
ALTER TABLE classification MODIFY type DEFAULT NULL;
ALTER TABLE classification MODIFY tenant_id NULL;
ALTER TABLE classification ADD project_id NUMBER(10,0);
merge into classification c using project p on (c.tenant_id=p.tenant_id) when matched then update set c.project_id=p.id;
ALTER TABLE classification ADD CONSTRAINT fk_clf_project FOREIGN KEY(project_id) REFERENCES project(id) ON DELETE CASCADE; 
ALTER TABLE classification DROP CONSTRAINT fk_tenant_clf_tenant;
ALTER TABLE classification ADD CONSTRAINT fk_clf_tenant FOREIGN KEY(tenant_id) REFERENCES tenant(id) ON DELETE CASCADE;
ALTER TABLE classification DROP CONSTRAINT u_tenant_clf_name;
ALTER TABLE classification ADD CONSTRAINT u_clf_name UNIQUE (tenant_id, project_id, name);

ALTER TABLE policy_component ADD type NUMBER(1,0) DEFAULT 0 NOT NULL ;
UPDATE policy_component
	SET type = (SELECT CASE
				WHEN t.parent_id IS NOT NULL THEN 1
				ELSE 0
			   END
FROM tenant t WHERE t.id = tenant_id);			   
ALTER TABLE policy_component MODIFY type DEFAULT NULL;
ALTER TABLE policy_component ADD project_id NUMBER(10,0);
merge into policy_component pc using project p on (p.tenant_id = pc.tenant_id) when matched then update set pc.project_id = p.id;
ALTER TABLE policy_component ADD CONSTRAINT fk_policy_component_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE;
ALTER TABLE policy_component MODIFY tenant_id NULL;

ALTER TABLE nxl_metadata ADD  token_group_name VARCHAR(250);
merge into  nxl_metadata using  tenant on (nxl_metadata.tenant_id=tenant.id) when matched then update set token_group_name=tenant.name;
ALTER TABLE nxl_metadata drop COLUMN tenant_id;

ALTER TABLE key_store_entry RENAME COLUMN tenant_name TO token_group_name;

ALTER TABLE tenant ADD keystore_id VARCHAR(36);
merge into tenant t using key_store_entry k on (k.token_group_name = t.name) when matched then update set t.keystore_id = k.id;
ALTER TABLE tenant MODIFY keystore_id NOT NULL;
ALTER TABLE tenant ADD CONSTRAINT fk_tenant_keystore FOREIGN KEY (keystore_id) REFERENCES key_store_entry (id) ON DELETE CASCADE;

ALTER TABLE project DROP COLUMN keystore;
ALTER TABLE project ADD keystore_id VARCHAR(36);
ALTER TABLE project ADD alias_project_name varchar(50);
UPDATE project p SET keystore_id = (select k.id from key_store_entry k, tenant t where k.token_group_name = t.name and t.id = p.tenant_id);
ALTER TABLE project MODIFY keystore_id NOT NULL;
merge into project using tenant on (project.tenant_id = tenant.id) when matched then update SET alias_project_name=tenant.name;
ALTER TABLE project RENAME COLUMN tenant_id TO parent_tenant_id;
ALTER TABLE project DROP CONSTRAINT u_project_name;
UPDATE project 
 SET parent_tenant_id = (select case WHEN t.parent_id IS NULL 
                          THEN t.id
                          ELSE t.parent_id END
                        FROM tenant t
                        WHERE t.id = parent_tenant_id);
ALTER TABLE project ADD CONSTRAINT fk_project_keystore FOREIGN KEY (keystore_id) REFERENCES key_store_entry (id) ON DELETE CASCADE;