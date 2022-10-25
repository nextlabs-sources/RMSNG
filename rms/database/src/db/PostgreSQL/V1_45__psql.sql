ALTER TABLE repository ALTER COLUMN ios_token TYPE character varying(6000);

ALTER TABLE sharing_transaction ALTER COLUMN device_id TYPE character varying(255);
ALTER TABLE client ALTER COLUMN device_id TYPE character varying(255);
ALTER TABLE activity_log ALTER COLUMN device_id TYPE character varying(255);

ALTER TABLE activity_log ALTER COLUMN file_path_id TYPE character varying(2000);
ALTER TABLE activity_log ALTER COLUMN file_path TYPE character varying(2000);