ALTER TABLE project DROP COLUMN classification_modified;
ALTER TABLE project ADD configuration_modified TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL;