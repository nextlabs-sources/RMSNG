ALTER TABLE repository DROP CONSTRAINT u_repository_account;
ALTER TABLE repository ADD CONSTRAINT u_repository_account UNIQUE (user_id, account_id, account_name);