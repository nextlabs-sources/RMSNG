ALTER TABLE project ADD COLUMN classification_modified timestamp WITH TIME ZONE NOT NULL DEFAULT now();
CREATE TABLE project_classification (
    id varchar(36) NOT NULL,
    name varchar(50) NOT NULL,
    project_id int NOT NULL,
    is_multi_sel boolean,
    is_mandatory boolean,
    labels varchar(1200) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT u_project_clf_name UNIQUE(project_id, name),
    CONSTRAINT fk_project_clf_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
);