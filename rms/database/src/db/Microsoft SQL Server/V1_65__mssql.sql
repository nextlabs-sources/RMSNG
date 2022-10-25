ALTER TABLE rms.project ADD classification_modified DATETIME2(6) NOT NULL DEFAULT getutcdate();
CREATE TABLE rms.project_classification
(
   id NVARCHAR(36) NOT NULL,
   name NVARCHAR(50) NOT NULL,
   project_id INT NOT NULL,
   is_multi_sel SMALLINT NOT NULL,
   is_mandatory SMALLINT NOT NULL,   
   labels NVARCHAR(1200) NOT NULL,
   CONSTRAINT PROJECT_CLF_PKEY PRIMARY KEY(id),
   CONSTRAINT U_PROJECT_CLF_NAME UNIQUE(project_id, name),
   CONSTRAINT FK_PROJECT_CLF_PROJECT FOREIGN KEY (project_id) 
   REFERENCES rms.project(id) ON DELETE CASCADE
);