ALTER TABLE project_classification ADD COLUMN parent_id VARCHAR(36);
ALTER TABLE project_classification ADD COLUMN order_id int NOT NULL;
ALTER TABLE project_classification ALTER COLUMN name TYPE VARCHAR(60);