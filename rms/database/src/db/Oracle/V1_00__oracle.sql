CREATE TABLE users
(
   id NUMBER(10,0) NOT NULL,
   display_name VARCHAR2(150),
   email VARCHAR2(255),
   attempt NUMBER(10,0)  DEFAULT 0 NOT NULL,
   creation_time TIMESTAMP(6) NOT NULL,
   status NUMBER(5,0)  DEFAULT 0 NOT NULL,
   type NUMBER(5,0),
   CONSTRAINT user_pkey PRIMARY KEY(id)
);

CREATE TABLE activity_log
(
   id VARCHAR2(36) NOT NULL,
   duid VARCHAR2(36) NOT NULL,
   owner VARCHAR2(150) NOT NULL,
   user_id NUMBER(10,0) NOT NULL,
   operation NUMBER(10,0) NOT NULL,
   device_id VARCHAR2(255),
   device_type NUMBER(10,0) NOT NULL,
   repository_id VARCHAR2(36),
   file_path_id VARCHAR2(2000),
   file_name VARCHAR2(255),
   file_path VARCHAR2(2000),
   app_name VARCHAR2(150),
   app_path VARCHAR2(512),
   app_publisher VARCHAR2(150),
   access_time TIMESTAMP(6) NOT NULL,
   access_result NUMBER(10,0) NOT NULL,
   activity_data CLOB,
   account_type NUMBER(5,0)  DEFAULT 0 NOT NULL,
   CONSTRAINT activity_log_pkey PRIMARY KEY(id),
   CONSTRAINT fk_activity_log_user FOREIGN KEY(user_id)
   REFERENCES users(id)  ON DELETE CASCADE
);

CREATE INDEX idx_duid_access_result
ON activity_log
(duid, 
access_result);

CREATE INDEX idx_duid_access_time
ON activity_log
(duid, 
access_time  DESC);

CREATE TABLE all_nxl
(
   duid VARCHAR2(36) NOT NULL,
   user_id NUMBER(10,0) NOT NULL,
   permissions NUMBER(10,0) NOT NULL,
   metadata VARCHAR2(4000),
   file_name VARCHAR2(255),
   display_name VARCHAR2(255),
   creation_time TIMESTAMP(6) NOT NULL,
   last_modified TIMESTAMP(6) NOT NULL,
   status NUMBER(5,0)  DEFAULT 0 NOT NULL,
   is_shared NUMBER(1,0) NOT NULL,
   owner VARCHAR2(150),
   policy CLOB,
   CONSTRAINT shared_nxl_pkey PRIMARY KEY(duid),
   CONSTRAINT fk_shared_nxl_user FOREIGN KEY(user_id)
   REFERENCES users(id)  ON DELETE CASCADE
);

CREATE INDEX idx_user_id
ON all_nxl
(user_id);

CREATE TABLE black_list
(
   duid VARCHAR2(36) NOT NULL,
   user_id NUMBER(10,0) NOT NULL,
   creation_time TIMESTAMP(6) NOT NULL,
   expiration TIMESTAMP(6) NOT NULL,
   CONSTRAINT black_list_pkey PRIMARY KEY(duid),
   CONSTRAINT fk_black_list_user FOREIGN KEY(user_id)
   REFERENCES users(id)  ON DELETE CASCADE
);

CREATE TABLE tenant
(
   id VARCHAR2(36) NOT NULL,
   name VARCHAR2(250),
   admin VARCHAR2(150),
   security_mode NUMBER(10,0) NOT NULL,
   dns_name VARCHAR2(150),
   display_name VARCHAR2(150),
   login_icon BLOB,
   preference VARCHAR2(2000) NOT NULL,
   creation_time TIMESTAMP(6) NOT NULL,
   CONSTRAINT tenant_pkey PRIMARY KEY(id),
   CONSTRAINT u_tenant_name UNIQUE(name)
);

CREATE TABLE storage_provider
(
   id VARCHAR2(36) NOT NULL,
   tenant_id VARCHAR2(36) NOT NULL,
   name VARCHAR2(150),
   type NUMBER(10,0) NOT NULL,
   attributes VARCHAR2(2000),
   creation_time TIMESTAMP(6) NOT NULL,
   CONSTRAINT storage_provider_pkey PRIMARY KEY(id),
   CONSTRAINT fk_storage_tenant FOREIGN KEY(tenant_id)
   REFERENCES tenant(id)  ON DELETE CASCADE
);

CREATE TABLE repository
(
   id VARCHAR2(36) NOT NULL,
   provider_id VARCHAR2(36) NOT NULL,
   user_id NUMBER(10,0) NOT NULL,
   name VARCHAR2(150),
   shared NUMBER(10,0) NOT NULL,
   account_name VARCHAR2(250),
   account_id VARCHAR2(250),
   token VARCHAR2(2000),
   ios_token CLOB,
   preference VARCHAR2(2000),
   creation_time TIMESTAMP(6) NOT NULL,
   last_updated_time TIMESTAMP(6),
   android_token VARCHAR2(2000),
   state VARCHAR2(1000),
   CONSTRAINT repository_pkey PRIMARY KEY(id),
   CONSTRAINT fk_repository_provider FOREIGN KEY(provider_id)
   REFERENCES storage_provider(id)  ON DELETE CASCADE,
   CONSTRAINT fk_repository_user FOREIGN KEY(user_id)
   REFERENCES users(id)  ON DELETE CASCADE,
   CONSTRAINT u_repository_account UNIQUE(user_id,account_id,account_name)
);

CREATE INDEX idx_repo_user_provider
ON repository
(user_id, provider_id);

CREATE TABLE builtin_repo_item
(
   id NUMBER(10,0) NOT NULL,
   repo_id VARCHAR2(36) NOT NULL,
   file_path VARCHAR2(2000),
   file_path_display VARCHAR2(2000),
   file_path_search VARCHAR2(255),
   last_modified TIMESTAMP(6) NOT NULL,
   is_dir NUMBER(1,0) NOT NULL,
   size_in_bytes NUMBER(19,0),
   custom_metadata VARCHAR2(2000),
   duid VARCHAR2(36),
   is_deleted NUMBER(1,0),
   parent_file_path_hash VARCHAR2(32) NOT NULL,
   CONSTRAINT builtin_repo_item_pkey PRIMARY KEY(id),
   CONSTRAINT fk_built_in_repo FOREIGN KEY(repo_id)
   REFERENCES repository(id)  ON DELETE CASCADE,
   CONSTRAINT u_file_path UNIQUE(repo_id,file_path)
);

CREATE INDEX idx_parent_file_path_hash
ON builtin_repo_item
(repo_id, parent_file_path_hash);

CREATE INDEX idx_duid
ON builtin_repo_Item
(duid);

CREATE TABLE client
(
   client_id VARCHAR2(32) NOT NULL,
   device_id VARCHAR2(255) NOT NULL,
   device_type NUMBER(10,0)  DEFAULT 0 NOT NULL,
   manufacturer VARCHAR2(50),
   model VARCHAR2(32),
   os_version VARCHAR2(64),
   app_name VARCHAR2(150),
   app_version VARCHAR2(20),
   push_token VARCHAR2(64),
   status NUMBER(10,0)  DEFAULT 0 NOT NULL,
   creation_date TIMESTAMP(6) NOT NULL,
   last_modified TIMESTAMP(6) NOT NULL,
   notes VARCHAR2(50),
   CONSTRAINT client_pkey PRIMARY KEY(client_id)
);

CREATE TABLE customer_account
(
   id NUMBER(19,0) NOT NULL,
   account_type VARCHAR2(50) NOT NULL,
   creation_time TIMESTAMP(6) NOT NULL,
   last_updated_time TIMESTAMP(6),
   payment_customer_id VARCHAR2(255),
   user_id NUMBER(10,0) NOT NULL,
   tenant_id VARCHAR2(36) NOT NULL,
   CONSTRAINT customer_account_pkey PRIMARY KEY(id),
   CONSTRAINT fk_customer_account_tenant FOREIGN KEY(tenant_id)
   REFERENCES tenant(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
,
   CONSTRAINT fk_customer_account_user FOREIGN KEY(user_id)
   REFERENCES users(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
);

CREATE TABLE favorite_file
(
   repository_id VARCHAR2(36) NOT NULL,
   file_path_id VARCHAR2(2000) NOT NULL,
   file_path VARCHAR2(2000) NOT NULL,
   last_modified TIMESTAMP(6) NOT NULL,
   status NUMBER(5,0)  DEFAULT 0 NOT NULL,
   file_path_search VARCHAR2(255) NOT NULL,
   file_last_modified TIMESTAMP(6),
   file_size NUMBER(19,0),
   id NUMBER(10,0) NOT NULL,
   parent_file_id_hash VARCHAR2(32) NOT NULL,
   file_path_id_hash VARCHAR2(32) NOT NULL,
   CONSTRAINT favorite_file_pkey PRIMARY KEY(id),
   CONSTRAINT fk_favorite_file FOREIGN KEY(repository_id)
   REFERENCES repository(id)  ON DELETE CASCADE,
   CONSTRAINT uk_fav_file_1 UNIQUE(repository_id,file_path_id)
);

CREATE INDEX idx_file_path_id_hash
ON favorite_file
(repository_id, 
file_path_id_hash, 
status  DESC);

CREATE INDEX idx_parent_file_id_hash
ON favorite_file
(repository_id, 
parent_file_id_hash, 
status  DESC);

CREATE INDEX idx_status_last_modified
ON favorite_file
(repository_id, 
status, 
last_modified);

CREATE TABLE key_store_entry
(
   id VARCHAR2(36) NOT NULL,
   creation_time TIMESTAMP(6) NOT NULL,
   credential VARCHAR2(255) NOT NULL,
   data BLOB NOT NULL,
   key_store_type VARCHAR2(15) NOT NULL,
   tenant_name VARCHAR2(250),
   version NUMBER(10,0) NOT NULL,
   CONSTRAINT key_store_entry_pkey PRIMARY KEY(id),
   CONSTRAINT uk_key_store_tenant UNIQUE(tenant_name)
);

CREATE TABLE login_account
(
   id NUMBER(10,0) NOT NULL,
   login_name VARCHAR2(150) NOT NULL,
   type NUMBER(10,0)  DEFAULT 0 NOT NULL,
   password BLOB,
   email VARCHAR2(255),
   user_id NUMBER(10,0) NOT NULL,
   attempt NUMBER(10,0)  DEFAULT 0 NOT NULL,
   otp BLOB,
   creation_time TIMESTAMP(6) NOT NULL,
   last_attempt TIMESTAMP(6),
   last_login TIMESTAMP(6),
   status NUMBER(5,0)  DEFAULT 0 NOT NULL,
   CONSTRAINT login_account_pkey PRIMARY KEY(id),
   CONSTRAINT fk_user_login_account FOREIGN KEY(user_id)
   REFERENCES users(id)  ON DELETE CASCADE,
   CONSTRAINT u_login_account UNIQUE(login_name,type)
);

CREATE TABLE payment_method
(
   id NUMBER(19,0) NOT NULL,
   payment_customer_id VARCHAR2(255),
   status NUMBER(10,0) NOT NULL,
   token VARCHAR2(255) NOT NULL,
   CONSTRAINT payment_method_pkey PRIMARY KEY(id)
);

CREATE TABLE subscription
(
   id NUMBER(19,0) NOT NULL,
   billing_cycle_length NUMBER(10,0) NOT NULL,
   billing_date TIMESTAMP(6),
   billing_status NUMBER(10,0) NOT NULL,
   no_of_billing_cycle NUMBER(10,0),
   subscription_id VARCHAR2(100) NOT NULL,
   trial_period DATE,
   payment_method_id NUMBER(19,0),
   CONSTRAINT subscription_pkey PRIMARY KEY(id),
   CONSTRAINT fk_subs_payment_method FOREIGN KEY(payment_method_id)
   REFERENCES payment_method(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
,
   CONSTRAINT subs_payment_method_id_key UNIQUE(payment_method_id)
);

CREATE TABLE project
(
   id NUMBER(10,0) NOT NULL,
   name VARCHAR2(50) NOT NULL,
   tenant_id VARCHAR2(36) NOT NULL,
   external_id VARCHAR2(50),
   type NUMBER(10,0)  DEFAULT 0 NOT NULL,
   keystore BLOB,
   display_name VARCHAR2(150) NOT NULL,
   description VARCHAR2(250),
   owner VARCHAR2(250),
   creation_time TIMESTAMP(6) NOT NULL,
   last_modified TIMESTAMP(6) NOT NULL,
   preferences VARCHAR2(2000),
   status NUMBER(5,0)  DEFAULT 0 NOT NULL,
   subscription_id NUMBER(19,0),
   customer_account_id NUMBER(19,0),
   default_invitation_msg VARCHAR2(250),
   CONSTRAINT project_pkey PRIMARY KEY(id),
   CONSTRAINT fk_project_customer_account FOREIGN KEY(customer_account_id)
   REFERENCES customer_account(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
,
   CONSTRAINT fk_project_subscription FOREIGN KEY(subscription_id)
   REFERENCES subscription(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
,
   CONSTRAINT fk_project_tenant FOREIGN KEY(tenant_id)
   REFERENCES tenant(id)  ON DELETE CASCADE,
   CONSTRAINT u_project_name UNIQUE(name,tenant_id)
);

CREATE TABLE membership
(
   name VARCHAR2(150) NOT NULL,
   project_id NUMBER(10,0) NOT NULL,
   tenant_id VARCHAR2(36) NOT NULL,
   user_id NUMBER(10,0) NOT NULL,
   external_id VARCHAR2(50),
   type NUMBER(10,0)  DEFAULT 0 NOT NULL,
   keystore BLOB,
   preferences VARCHAR2(2000),
   creation_time TIMESTAMP(6) NOT NULL,
   last_modified TIMESTAMP(6) NOT NULL,
   status NUMBER(5,0)  DEFAULT 0 NOT NULL,
   inviter_id NUMBER(10,0),
   invited_on TIMESTAMP(6),
   project_action_time TIMESTAMP(6)  DEFAULT CAST('1-JAN-1970 12:00:00 AM' AS TIMESTAMP(6)) NOT NULL,
   CONSTRAINT membership_pkey PRIMARY KEY(name),
   CONSTRAINT fk_membership_project FOREIGN KEY(project_id)
   REFERENCES project(id)  ON DELETE CASCADE,
   CONSTRAINT fk_membership_tenant FOREIGN KEY(tenant_id)
   REFERENCES tenant(id)  ON DELETE CASCADE,
   CONSTRAINT fk_membership_user FOREIGN KEY(user_id)
   REFERENCES users(id)  ON DELETE CASCADE
);

CREATE TABLE project_invitation
(
   id NUMBER(19,0) NOT NULL,
   project_id NUMBER(10,0) NOT NULL,
   inviter_id NUMBER(10,0) NOT NULL,
   invitee_email VARCHAR2(150) NOT NULL,
   invite_time TIMESTAMP(6) NOT NULL,
   expire_date TIMESTAMP(6) NOT NULL,
   action_time TIMESTAMP(6),
   status NUMBER(5,0)  DEFAULT 0 NOT NULL,
   comments VARCHAR2(250),
   invitation_msg VARCHAR2(250),
   CONSTRAINT project_invitation_pkey PRIMARY KEY(id),
   CONSTRAINT fk_pi_project FOREIGN KEY(project_id)
   REFERENCES project(id)  ON DELETE CASCADE,
   CONSTRAINT fk_pi_user FOREIGN KEY(inviter_id)
   REFERENCES users(id)  ON DELETE CASCADE
);

CREATE TABLE project_space_item
(
   id VARCHAR2(36) NOT NULL,
   duid VARCHAR2(36),
   user_id NUMBER(10,0) NOT NULL,
   project_id NUMBER(10,0) NOT NULL,
   permissions NUMBER(10,0),
   file_path VARCHAR2(2000),
   file_path_display VARCHAR2(2000),
   file_path_search VARCHAR2(255),
   creation_time TIMESTAMP(6) NOT NULL,
   expiration TIMESTAMP(6),
   last_modified TIMESTAMP(6) NOT NULL,
   is_dir NUMBER(1,0)  DEFAULT 0 NOT NULL,
   size_in_bytes NUMBER(19,0),
   file_parent_path VARCHAR2(2000),
   CONSTRAINT project_space_item_pkey PRIMARY KEY(id),
   CONSTRAINT fk_projectspace_project FOREIGN KEY(project_id)
   REFERENCES project(id)  ON DELETE CASCADE,
   CONSTRAINT fk_projectspace_user FOREIGN KEY(user_id)
   REFERENCES users(id)  ON DELETE CASCADE,
   CONSTRAINT u_file_path_project UNIQUE(project_id,file_path)
);

CREATE TABLE resource_lock
(
   id VARCHAR2(255) NOT NULL,
   last_updated TIMESTAMP(6) NOT NULL,
   status NUMBER(5,0)  DEFAULT 0 NOT NULL,
   CONSTRAINT resource_lock_pkey PRIMARY KEY(id)
);

CREATE TABLE sharing_transaction
(
   id VARCHAR2(36) NOT NULL,
   duid VARCHAR2(36) NOT NULL,
   user_id NUMBER(10,0) NOT NULL,
   repository_id VARCHAR2(36),
   file_path_id VARCHAR2(1000),
   file_path VARCHAR2(1000),
   device_id VARCHAR2(255),
   device_type NUMBER(10,0) NOT NULL,
   creation_time TIMESTAMP(6) NOT NULL,
   status NUMBER(5,0)  DEFAULT 0 NOT NULL,
   comments VARCHAR2(250),
   CONSTRAINT sharing_transaction_pkey PRIMARY KEY(id),
   CONSTRAINT fk_sharing_nxl FOREIGN KEY(duid)
   REFERENCES all_nxl(duid)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
,
   CONSTRAINT fk_sharing_repository FOREIGN KEY(repository_id)
   REFERENCES repository(id)  ON DELETE SET NULL,
   CONSTRAINT fk_sharing_user FOREIGN KEY(user_id)
   REFERENCES users(id)  ON DELETE CASCADE
);

CREATE INDEX idx_duid_share_txn
ON sharing_transaction
(duid);

CREATE TABLE sharing_recipient
(
   duid VARCHAR2(36) NOT NULL,
   email VARCHAR2(255) NOT NULL,
   transaction_id VARCHAR2(36) NOT NULL,
   last_modified TIMESTAMP(6) NOT NULL,
   status NUMBER(5,0)  DEFAULT 0 NOT NULL,
   CONSTRAINT sharing_recipient_pkey PRIMARY KEY(duid,email),
   CONSTRAINT fk_recipient_transaction FOREIGN KEY(transaction_id)
   REFERENCES sharing_transaction(id)  ON DELETE CASCADE
);

CREATE INDEX idx_duid_share_recpt
ON sharing_recipient
(duid);

CREATE TABLE task_status
(
   id VARCHAR2(255) NOT NULL,
   last_successful_update TIMESTAMP(6) NOT NULL,
   last_failed_update TIMESTAMP(6) NOT NULL,
   status NUMBER(5,0)  DEFAULT 0 NOT NULL,
   CONSTRAINT task_status_pkey PRIMARY KEY(id)
);

CREATE TABLE user_preferences
(
   id NUMBER(10,0) NOT NULL,
   expiry CLOB,
   watermark VARCHAR2(255),
   preferences CLOB,
   CONSTRAINT user_preferences_pkey PRIMARY KEY(id),
   CONSTRAINT fk_user_id FOREIGN KEY(id)
   REFERENCES users(id)  ON DELETE CASCADE
);

CREATE TABLE user_session
(
   id NUMBER(19,0) NOT NULL,
   client_id VARCHAR2(32) NOT NULL,
   creation_time TIMESTAMP(6) NOT NULL,
   device_type NUMBER(10,0) NOT NULL,
   expiration_time TIMESTAMP(6) NOT NULL,
   login_type NUMBER(10,0) NOT NULL,
   status NUMBER(10,0) NOT NULL,
   ticket BLOB NOT NULL,
   ttl NUMBER(19,0) NOT NULL,
   user_id NUMBER(10,0) NOT NULL,
   CONSTRAINT user_session_pkey PRIMARY KEY(id),
   CONSTRAINT fk_user_session_user FOREIGN KEY(user_id)
   REFERENCES users(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
);

CREATE INDEX idx_user_session_1
ON user_session
(user_id, 
status, 
client_id, 
device_type);

CREATE TABLE feedback
(
  id VARCHAR2(36) NOT NULL,
  type VARCHAR2(250) NOT NULL,
  summary VARCHAR2(100) NOT NULL, -- size from UI
  description VARCHAR2(2000) NOT NULL, -- size from UI
  client_id VARCHAR2(32) NOT NULL,
  device_id VARCHAR2(32),
  device_type NUMBER(10,0) NOT NULL,
  user_id NUMBER(10,0) NOT NULL,
  creation_time TIMESTAMP(6) NOT NULL,
  CONSTRAINT feedback_pkey PRIMARY KEY(id)
);

CREATE SEQUENCE  "BUILTIN_REPO_ITEM_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;
CREATE SEQUENCE  "CSN"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 21 CACHE 20 NOORDER  NOCYCLE ;
CREATE SEQUENCE  "CUST_ACCT_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;
CREATE SEQUENCE  "CUSTOMER_ACCOUNT_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;
CREATE SEQUENCE  "FAVORITE_FILE_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;
CREATE SEQUENCE  "LOGIN_ACCOUNT_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;
CREATE SEQUENCE  "PAYMENT_METHOD_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;
CREATE SEQUENCE  "PROJECT_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;
CREATE SEQUENCE  "PROJECT_INVITATION_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;
CREATE SEQUENCE  "SUBSCRIPTION_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;
CREATE SEQUENCE  "USER_ID_SEQ"  MINVALUE 1 MAXVALUE 9223372036854775807 INCREMENT BY 1 START WITH 62 CACHE 10 NOORDER  NOCYCLE ;
CREATE SEQUENCE  "USER_PREFERENCES_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;
CREATE SEQUENCE  "USER_SESSION_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE  NOORDER  NOCYCLE ;




