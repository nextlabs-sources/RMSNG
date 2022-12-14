ALTER TABLE "group" RENAME TO "project";
ALTER TABLE membership DROP CONSTRAINT fk_membership_group;
ALTER TABLE "project" DROP CONSTRAINT group_pkey;
ALTER TABLE "project" ADD CONSTRAINT project_pkey PRIMARY KEY (id);
ALTER TABLE "project" DROP CONSTRAINT fk_group_tenant;
ALTER TABLE "project" ADD CONSTRAINT fk_project_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id) ON UPDATE NO ACTION ON DELETE CASCADE;
ALTER TABLE "project" DROP CONSTRAINT u_group_name;
ALTER TABLE "project" ADD CONSTRAINT u_project_name UNIQUE (name, tenant_id);
ALTER TABLE membership RENAME COLUMN group_id TO project_id;
ALTER TABLE membership ADD CONSTRAINT fk_membership_project FOREIGN KEY (project_id) REFERENCES "project"(id) ON UPDATE NO ACTION ON DELETE CASCADE;
