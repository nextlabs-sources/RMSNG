ALTER TABLE project DROP COLUMN classification_modified;
ALTER TABLE project ADD COLUMN configuration_modified timestamp WITH TIME ZONE NOT NULL DEFAULT now();