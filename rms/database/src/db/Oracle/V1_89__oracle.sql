ALTER TABLE REPOSITORY ADD CLASS INTEGER DEFAULT 0 NOT NULL;
COMMENT ON COLUMN REPOSITORY.CLASS IS 'CLASS - 0 for PERSONAL, 1 for APPLICATION, 2 for BUSINESS';