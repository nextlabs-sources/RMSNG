ALTER TABLE project_classification ADD parent_id VARCHAR2(36);
ALTER TABLE project_classification ADD order_id NUMBER(10,0) NOT NULL;
ALTER TABLE project_classification MODIFY name VARCHAR2(60);