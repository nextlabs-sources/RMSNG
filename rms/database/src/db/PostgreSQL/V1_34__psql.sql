ALTER TABLE sharing_transaction ALTER COLUMN device_id TYPE character varying(50);
ALTER TABLE activity_log ALTER COLUMN device_id TYPE character varying(50);
ALTER TABLE client ALTER COLUMN device_id TYPE character varying(50);
