ALTER TABLE task_status DROP CONSTRAINT task_status_pkey;
ALTER TABLE task_status RENAME COLUMN resource to id;
ALTER TABLE task_status ADD PRIMARY KEY (id);