ALTER TABLE activity_log ADD COLUMN account_type smallint NOT NULL DEFAULT 0;
UPDATE activity_log SET account_type = 1 WHERE duid IN (SELECT duid FROM project_space_item);