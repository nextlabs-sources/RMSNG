ALTER TABLE rms.repository 
ADD "class" int4 NOT NULL DEFAULT 0;
COMMENT ON COLUMN rms.repository."class" IS 'CLASS - 0 for PERSONAL, 1 for APPLICATION, 2 for BUSINESS';