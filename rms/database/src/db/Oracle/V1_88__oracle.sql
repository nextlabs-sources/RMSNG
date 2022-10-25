-- This table will be used to store all nxl files protected in external repository
CREATE TABLE external_repository_nxl (
   duid VARCHAR2(36)  NOT NULL,
   user_id INT  NOT NULL,
   repository_id VARCHAR2(36) NOT NULL,
   permissions INT  NOT NULL,
   file_path VARCHAR2(2000) NOT NULL,
   file_name VARCHAR2(255) NOT NULL,
   display_name VARCHAR2(255) NOT NULL,
   creation_time TIMESTAMP(6) NOT NULL,
   last_modified TIMESTAMP(6) NOT NULL,
   status INT  DEFAULT 0  NOT NULL,
   is_shared  NUMBER(1,0)  NOT NULL,
   owner VARCHAR2(150),
   PRIMARY KEY(duid),
   CONSTRAINT FK_EXTNL_REPO_NXL_USER FOREIGN KEY(user_id) REFERENCES users(id),
   CONSTRAINT FK_EXTNL_REPO_ID FOREIGN KEY(repository_id) REFERENCES repository(id)  ON DELETE CASCADE
);

CREATE INDEX IDX_EXTNL_REPO_USER_ID
ON external_repository_nxl
(user_id);

CREATE INDEX IDX_EXTNL_REPOSITORY_ID
ON external_repository_nxl
(repository_id);
