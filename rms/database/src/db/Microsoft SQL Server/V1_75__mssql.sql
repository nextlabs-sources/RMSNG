
CREATE TABLE rms.tag (
  id int IDENTITY NOT NULL,
  tenant_id NVARCHAR(36) NOT NULL,
  name NVARCHAR(150) NOT NULL,
  type int,
  description NVARCHAR(2000),
  order_id int NOT NULL,
  creation_time DATETIME2(6) NOT NULL,
  last_modified DATETIME2(6) NOT NULL,
  CONSTRAINT tag_pkey PRIMARY KEY (id),
  CONSTRAINT u_tag_name UNIQUE (name, tenant_id,type),
  CONSTRAINT u_tag_order UNIQUE (tenant_id, order_id,type),
  CONSTRAINT fk_tags_tenant FOREIGN KEY (tenant_id) REFERENCES rms.tenant(id)
);

CREATE TABLE rms.project_tag (
  id int IDENTITY NOT NULL,
  tag_id int not null,
  project_id int not null,
  CONSTRAINT project_tag_pkey PRIMARY KEY (id),
  CONSTRAINT u_tag_project_id UNIQUE (tag_id, project_id),
  CONSTRAINT fk_project_tags_project FOREIGN KEY (project_id) REFERENCES rms.project (id),
  CONSTRAINT fk_project_tags_tag FOREIGN KEY (tag_id) REFERENCES rms.tag(id)
);