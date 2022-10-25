ALTER TABLE project_space_item ADD status NUMBER(5,0) DEFAULT 0 NOT NULL;
ALTER TABLE sharing_transaction ADD src_project_id NUMBER(10,0) ;