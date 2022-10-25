ALTER TABLE project_invitation ALTER COLUMN expire_time TYPE date;
ALTER TABLE project_invitation RENAME COLUMN expire_time TO expire_date;