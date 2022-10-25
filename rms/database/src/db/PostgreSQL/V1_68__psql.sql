ALTER TABLE project
 ADD COLUMN expiry text,
 ADD COLUMN watermark character varying(255);