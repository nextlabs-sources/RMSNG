ALTER TABLE rms.project_classification ADD parent_id NVARCHAR(36);
ALTER TABLE rms.project_classification ADD order_id INT NOT NULL;
ALTER TABLE rms.project_classification ALTER COLUMN name NVARCHAR(60);