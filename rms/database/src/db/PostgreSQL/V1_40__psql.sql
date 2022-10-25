ALTER TABLE favorite_file DROP COLUMN id;

ALTER TABLE favorite_file ADD COLUMN id SERIAL PRIMARY KEY;

ALTER TABLE favorite_file ALTER COLUMN file_path_id TYPE character varying(2000);
ALTER TABLE favorite_file ALTER COLUMN file_path_id SET NOT NULL;

ALTER TABLE favorite_file ALTER COLUMN file_path TYPE character varying(2000);
ALTER TABLE favorite_file ALTER COLUMN file_path SET NOT NULL;

ALTER TABLE offline_file DROP COLUMN id;

ALTER TABLE offline_file ADD COLUMN id SERIAL PRIMARY KEY;

ALTER TABLE offline_file ALTER COLUMN file_path_id TYPE character varying(2000);
ALTER TABLE offline_file ALTER COLUMN file_path_id SET NOT NULL;

ALTER TABLE offline_file ALTER COLUMN file_path TYPE character varying(2000);
ALTER TABLE offline_file ALTER COLUMN file_path SET NOT NULL;