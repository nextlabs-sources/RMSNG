ALTER TABLE rms.project_space_item ADD status  SMALLINT NOT NULL DEFAULT 0;
go
ALTER TABLE rms.sharing_transaction ADD src_project_id INT ;
go