ALTER TABLE repository RENAME COLUMN rmc_token TO ios_token;
ALTER TABLE repository ADD COLUMN android_token varchar(512);