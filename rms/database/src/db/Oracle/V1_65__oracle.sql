ALTER TABLE project ADD classification_modified TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL;
CREATE TABLE project_classification (
    id VARCHAR2(36) NOT NULL,
    name VARCHAR2(50) NOT NULL,
    project_id NUMBER(10,0) NOT NULL,
    is_multi_sel NUMBER(1,0) NOT NULL,
    is_mandatory NUMBER(1,0) NOT NULL,
    labels VARCHAR2(1200) NOT NULL,
    CONSTRAINT u_project_clf_name UNIQUE(project_id, name),
    CONSTRAINT project_clf_pkey PRIMARY KEY (id),
    CONSTRAINT fk_project_clf_project FOREIGN KEY (project_id)
    REFERENCES project(id) ON DELETE CASCADE
);