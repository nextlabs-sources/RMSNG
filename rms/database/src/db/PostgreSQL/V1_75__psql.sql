CREATE SEQUENCE tag_id_seq;
CREATE SEQUENCE project_tag_id_seq;
CREATE TABLE tag (
  id SERIAL,
  tenant_id varchar(36) not null,
  name varchar(150) not null,
  type int,
  description varchar(2000),
  order_id int not null,
  creation_time timestamp WITH TIME ZONE NOT NULL DEFAULT now(),
  last_modified timestamp WITH TIME ZONE NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT u_tag_name UNIQUE (name, tenant_id,type),
  CONSTRAINT u_tag_order UNIQUE (tenant_id, order_id,type),
  CONSTRAINT fk_tags_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id) ON DELETE CASCADE
);


CREATE TABLE project_tag (
  id SERIAL,
  tag_id int not null,
  project_id int not null,
  PRIMARY KEY (id),
  CONSTRAINT u_tag_project_id UNIQUE (tag_id, project_id),
  CONSTRAINT fk_project_tags_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
  CONSTRAINT fk_project_tags_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
);


