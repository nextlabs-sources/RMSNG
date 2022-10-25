CREATE TABLE rms.users
(
   id INT IDENTITY NOT NULL,
   display_name NVARCHAR(150),
   email NVARCHAR(255),
   attempt INT  DEFAULT 0  NOT NULL,
   creation_time DATETIME2(6)  NOT NULL,
   status INT  DEFAULT 0  NOT NULL,
   type INT,
   CONSTRAINT USER_PKEY PRIMARY KEY(id)
);

CREATE TABLE rms.activity_log
(
   id NVARCHAR(36)  NOT NULL,
   duid NVARCHAR(36)  NOT NULL,
   owner NVARCHAR(150)  NOT NULL,
   user_id INT  NOT NULL,
   operation INT  NOT NULL,
   device_id NVARCHAR(255),
   device_type INT  NOT NULL,
   repository_id NVARCHAR(36),
   file_path_id NVARCHAR(2000),
   file_name NVARCHAR(255),
   file_path NVARCHAR(1000),
   app_name NVARCHAR(150),
   app_path NVARCHAR(512),
   app_publisher NVARCHAR(150),
   access_time DATETIME2(6)  NOT NULL,
   access_result INT  NOT NULL,
   activity_data NVARCHAR(MAX),
   account_type INT  DEFAULT 0  NOT NULL,
   CONSTRAINT ACTIVITY_LOG_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_ACTIVITY_LOG_USER FOREIGN KEY(user_id)
   REFERENCES rms.users(id)  ON DELETE CASCADE
);

CREATE INDEX IDX_DUID_ACCESS_RESULT
ON rms.activity_log
(duid, 
access_result);

CREATE INDEX IDX_DUID_ACCESS_TIME
ON rms.activity_log
(duid, 
access_time  DESC);

CREATE TABLE rms.all_nxl
(
   duid NVARCHAR(36)  NOT NULL,
   user_id INT  NOT NULL,
   permissions INT  NOT NULL,
   metadata NVARCHAR(4000),
   file_name NVARCHAR(255),
   display_name NVARCHAR(255),
   creation_time DATETIME2(6)  NOT NULL,
   last_modified DATETIME2(6)  NOT NULL,
   status INT  DEFAULT 0  NOT NULL,
   is_shared SMALLINT  NOT NULL,
   owner NVARCHAR(150),
   policy NVARCHAR(MAX),
   CONSTRAINT SHARED_NXL_PKEY PRIMARY KEY(duid),
   CONSTRAINT FK_SHARED_NXL_USER FOREIGN KEY(user_id)
   REFERENCES rms.users(id)  ON DELETE CASCADE
);

CREATE INDEX IDX_USER_ID
ON rms.all_nxl
(user_id);

CREATE TABLE rms.black_list
(
   duid NVARCHAR(36)  NOT NULL,
   user_id INT  NOT NULL,
   creation_time DATETIME2(6)  NOT NULL,
   expiration DATETIME2(6)  NOT NULL,
   CONSTRAINT BLACK_LIST_PKEY PRIMARY KEY(duid),
   CONSTRAINT FK_BLACK_LIST_USER FOREIGN KEY(user_id)
   REFERENCES rms.users(id)  ON DELETE CASCADE
);

CREATE TABLE rms.tenant
(
   id NVARCHAR(36)  NOT NULL,
   name NVARCHAR(250),
   admin NVARCHAR(150),
   security_mode INT  NOT NULL,
   dns_name NVARCHAR(150),
   display_name NVARCHAR(150),
   login_icon VARBINARY(MAX),
   preference NVARCHAR(2000)  NOT NULL,
   creation_time DATETIME2(6)  NOT NULL,
   CONSTRAINT TENANT_PKEY PRIMARY KEY(id),
   CONSTRAINT U_TENANT_NAME UNIQUE(name)
);

CREATE TABLE rms.storage_provider
(
   id NVARCHAR(36)  NOT NULL,
   tenant_id NVARCHAR(36)  NOT NULL,
   name NVARCHAR(150),
   type INT  NOT NULL,
   attributes NVARCHAR(2000),
   creation_time DATETIME2(6)  NOT NULL,
   CONSTRAINT STORAGE_PROVIDER_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_STORAGE_TENANT FOREIGN KEY(tenant_id)
   REFERENCES rms.tenant(id)  ON DELETE CASCADE
);

CREATE TABLE rms.repository
(
   id NVARCHAR(36)  NOT NULL,
   provider_id NVARCHAR(36)  NOT NULL,
   user_id INT  NOT NULL,
   name NVARCHAR(150),
   shared INT  NOT NULL,
   account_name NVARCHAR(250),
   account_id NVARCHAR(250),
   token NVARCHAR(2000),
   ios_token VARCHAR(6000),
   preference NVARCHAR(2000),
   creation_time DATETIME2(6)  NOT NULL,
   last_updated_time DATETIME2(6),
   android_token NVARCHAR(2000),
   state NVARCHAR(1000),
   CONSTRAINT REPOSITORY_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_REPOSITORY_PROVIDER FOREIGN KEY(provider_id)
   REFERENCES rms.storage_provider(id)  ON DELETE CASCADE,
   CONSTRAINT FK_REPOSITORY_USER FOREIGN KEY(user_id)
   REFERENCES rms.users(id)  ON DELETE CASCADE,
   CONSTRAINT U_REPOSITORY_ACCOUNT UNIQUE(user_id,account_id,account_name)
);

CREATE INDEX IDX_REPO_USER_PROVIDER
ON rms.repository
(user_id, provider_id);

CREATE TABLE rms.builtin_repo_item
(
   id INT IDENTITY NOT NULL,
   repo_id NVARCHAR(36) NOT NULL,
   file_path NVARCHAR(2000),
   file_path_display NVARCHAR(2000),
   file_path_search NVARCHAR(255),
   last_modified DATETIME2(6)  NOT NULL,
   is_dir SMALLINT  NOT NULL,
   size_in_bytes BIGINT,
   custom_metadata NVARCHAR(2000),
   duid NVARCHAR(36),
   is_deleted SMALLINT,
   parent_file_path_hash NVARCHAR(32)  NOT NULL,
   CONSTRAINT BUILTIN_REPO_ITEM_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_BUILT_IN_REPO FOREIGN KEY(repo_id)
   REFERENCES rms.repository(id)  ON DELETE CASCADE,
   CONSTRAINT U_FILE_PATH UNIQUE(repo_id,file_path)
);

CREATE INDEX IDX_PARENT_FILE_PATH_HASH
ON rms.builtin_repo_item
(repo_id, parent_file_path_hash);

CREATE INDEX IDX_DUID
ON rms.builtin_repo_Item
(duid);

CREATE TABLE rms.client
(
   client_id NVARCHAR(32)  NOT NULL,
   device_id NVARCHAR(255)  NOT NULL,
   device_type INT  DEFAULT 0  NOT NULL,
   manufacturer NVARCHAR(50),
   model NVARCHAR(32),
   os_version NVARCHAR(64),
   app_name NVARCHAR(150),
   app_version NVARCHAR(20),
   push_token NVARCHAR(64),
   status INT  DEFAULT 0  NOT NULL,
   creation_date DATETIME2(6)  NOT NULL,
   last_modified DATETIME2(6)  NOT NULL,
   notes NVARCHAR(50),
   CONSTRAINT CLIENT_PKEY PRIMARY KEY(client_id)
);

CREATE TABLE rms.customer_account
(
   id BIGINT IDENTITY NOT NULL,
   account_type NVARCHAR(50)  NOT NULL,
   creation_time DATETIME2(6)  NOT NULL,
   last_updated_time DATETIME2(6),
   payment_customer_id NVARCHAR(255),
   user_id INT  NOT NULL,
   tenant_id NVARCHAR(36)  NOT NULL,
   CONSTRAINT CUSTOMER_ACCOUNT_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_CUSTOMER_ACCOUNT_TENANT FOREIGN KEY(tenant_id)
   REFERENCES rms.tenant(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
,
   CONSTRAINT FK_CUSTOMER_ACCOUNT_USER FOREIGN KEY(user_id)
   REFERENCES rms.users(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
);

CREATE TABLE rms.favorite_file
(
   repository_id NVARCHAR(36)  NOT NULL,
   file_path_id NVARCHAR(2000)  NOT NULL,
   file_path NVARCHAR(2000)  NOT NULL,
   last_modified DATETIME2(6)  NOT NULL,
   status INT  DEFAULT 0  NOT NULL,
   file_path_search NVARCHAR(255)  NOT NULL,
   file_last_modified DATETIME2(6),
   file_size BIGINT,
   id INT IDENTITY NOT NULL,
   parent_file_id_hash NVARCHAR(32)  NOT NULL,
   file_path_id_hash NVARCHAR(32)  NOT NULL,
   CONSTRAINT FAVORITE_FILE_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_FAVORITE_FILE FOREIGN KEY(repository_id)
   REFERENCES rms.repository(id)  ON DELETE CASCADE,
   CONSTRAINT UK_FAV_FILE_1 UNIQUE(repository_id,file_path_id)
);


CREATE INDEX IDX_FILE_PATH_ID_HASH
ON rms.favorite_file
(repository_id, 
file_path_id_hash, 
status  DESC);

CREATE INDEX IDX_PARENT_FILE_ID_HASH
ON rms.favorite_file
(repository_id, 
parent_file_id_hash, 
status  DESC);

CREATE INDEX IDX_STATUS_LAST_MODIFIED
ON rms.favorite_file
(repository_id, 
status, 
last_modified);

CREATE TABLE rms.key_store_entry
(
   id NVARCHAR(36)  NOT NULL,
   creation_time DATETIME2(6)  NOT NULL,
   credential NVARCHAR(255)  NOT NULL,
   data VARBINARY(MAX)  NOT NULL,
   key_store_type NVARCHAR(15)  NOT NULL,
   tenant_name NVARCHAR(250),
   version INT  NOT NULL,
   CONSTRAINT KEY_STORE_ENTRY_PKEY PRIMARY KEY(id),
   CONSTRAINT UK_KEY_STORE_TENANT UNIQUE(tenant_name)
);

CREATE TABLE rms.login_account
(
   id INT IDENTITY NOT NULL,
   login_name NVARCHAR(150)  NOT NULL,
   type INT  DEFAULT 0  NOT NULL,
   password VARBINARY(MAX),
   email NVARCHAR(255),
   user_id INT  NOT NULL,
   attempt INT  DEFAULT 0  NOT NULL,
   otp VARBINARY(MAX),
   creation_time DATETIME2(6)  NOT NULL,
   last_attempt DATETIME2(6),
   last_login DATETIME2(6),
   status INT  DEFAULT 0  NOT NULL,
   CONSTRAINT LOGIN_ACCOUNT_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_USER_LOGIN_ACCOUNT FOREIGN KEY(user_id)
   REFERENCES rms.users(id)  ON DELETE CASCADE,
   CONSTRAINT U_LOGIN_ACCOUNT UNIQUE(login_name,type)
);

CREATE TABLE rms.payment_method
(
   id BIGINT IDENTITY NOT NULL,
   payment_customer_id NVARCHAR(255),
   status INT  NOT NULL,
   token NVARCHAR(255)  NOT NULL,
   CONSTRAINT PAYMENT_METHOD_PKEY PRIMARY KEY(id)
);

CREATE TABLE rms.subscription
(
   id BIGINT IDENTITY NOT NULL,
   billing_cycle_length INT  NOT NULL,
   billing_date DATETIME2(6),
   billing_status INT  NOT NULL,
   no_of_billing_cycle INT,
   subscription_id NVARCHAR(100)  NOT NULL,
   trial_period DATE,
   payment_method_id BIGINT,
   CONSTRAINT SUBSCRIPTION_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_SUBS_PAYMENT_METHOD FOREIGN KEY(payment_method_id)
   REFERENCES rms.payment_method(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
,
   CONSTRAINT SUBS_PAYMENT_METHOD_ID_KEY UNIQUE(payment_method_id)
);

CREATE TABLE rms.project
(
   id INT IDENTITY NOT NULL,
   name NVARCHAR(50)  NOT NULL,
   tenant_id NVARCHAR(36)  NOT NULL,
   external_id NVARCHAR(50),
   type INT  DEFAULT 0  NOT NULL,
   keystore VARBINARY(MAX),
   display_name NVARCHAR(150)  NOT NULL,
   description NVARCHAR(250),
   owner NVARCHAR(250),
   creation_time DATETIME2(6)  NOT NULL,
   last_modified DATETIME2(6)  NOT NULL,
   preferences NVARCHAR(2000),
   status INT  DEFAULT 0  NOT NULL,
   subscription_id BIGINT,
   customer_account_id BIGINT,
   default_invitation_msg NVARCHAR(250),
   CONSTRAINT PROJECT_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_PROJECT_CUSTOMER_ACCOUNT FOREIGN KEY(customer_account_id)
   REFERENCES rms.customer_account(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
,
   CONSTRAINT FK_PROJECT_SUBSCRIPTION FOREIGN KEY(subscription_id)
   REFERENCES rms.subscription(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
,
   CONSTRAINT FK_PROJECT_TENANT FOREIGN KEY(tenant_id)
   REFERENCES rms.tenant(id)  ON DELETE CASCADE,
   CONSTRAINT U_PROJECT_NAME UNIQUE(name,tenant_id)
);

CREATE TABLE rms.membership
(
   name NVARCHAR(150)  NOT NULL,
   project_id INT  NOT NULL,
   tenant_id NVARCHAR(36)  NOT NULL,
   user_id INT  NOT NULL,
   external_id NVARCHAR(50),
   type INT  DEFAULT 0  NOT NULL,
   keystore VARBINARY(MAX),
   preferences NVARCHAR(2000),
   creation_time DATETIME2(6)  NOT NULL,
   last_modified DATETIME2(6)  NOT NULL,
   status INT  DEFAULT 0  NOT NULL,
   inviter_id INT,
   invited_on DATETIME2(6),
   project_action_time DATETIME2(6)  DEFAULT CAST('1-JAN-1970 12:00:00 AM' AS DATETIME2(6))  NOT NULL,
   CONSTRAINT MEMBERSHIP_PKEY PRIMARY KEY(name),
   CONSTRAINT FK_MEMBERSHIP_PROJECT FOREIGN KEY(project_id)
   REFERENCES rms.project(id)  ON DELETE CASCADE,
   CONSTRAINT FK_MEMBERSHIP_TENANT FOREIGN KEY(tenant_id)
   REFERENCES rms.tenant(id)  ON DELETE NO ACTION,
   CONSTRAINT FK_MEMBERSHIP_USER FOREIGN KEY(user_id)
   REFERENCES rms.users(id)  ON DELETE CASCADE
);

CREATE TABLE rms.project_invitation
(
   id BIGINT IDENTITY NOT NULL,
   project_id INT  NOT NULL,
   inviter_id INT  NOT NULL,
   invitee_email NVARCHAR(150)  NOT NULL,
   invite_time DATETIME2(6)  NOT NULL,
   expire_date DATETIME2(6)  NOT NULL,
   action_time DATETIME2(6),
   status INT  DEFAULT 0  NOT NULL,
   comments NVARCHAR(250),
   invitation_msg NVARCHAR(250),
   CONSTRAINT PROJECT_INVITATION_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_PI_PROJECT FOREIGN KEY(project_id)
   REFERENCES rms.project(id)  ON DELETE CASCADE,
   CONSTRAINT FK_PI_USER FOREIGN KEY(inviter_id)
   REFERENCES rms.users(id)  ON DELETE CASCADE
);

CREATE TABLE rms.project_space_item
(
   id NVARCHAR(36)  NOT NULL,
   duid NVARCHAR(36),
   user_id INT  NOT NULL,
   project_id INT  NOT NULL,
   permissions INT,
   file_path NVARCHAR(2000),
   file_path_display NVARCHAR(2000),
   file_path_search NVARCHAR(255),
   creation_time DATETIME2(6)  NOT NULL,
   expiration DATETIME2(6),
   last_modified DATETIME2(6)  NOT NULL,
   is_dir SMALLINT  DEFAULT 0  NOT NULL,
   size_in_bytes BIGINT,
   file_parent_path NVARCHAR(2000),
   CONSTRAINT PROJECT_SPACE_ITEM_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_PROJECTSPACE_PROJECT FOREIGN KEY(project_id)
   REFERENCES rms.project(id)  ON DELETE CASCADE,
   CONSTRAINT FK_PROJECTSPACE_USER FOREIGN KEY(user_id)
   REFERENCES rms.users(id)  ON DELETE CASCADE,
   CONSTRAINT U_FILE_PATH_PROJECT UNIQUE(project_id,file_path)
);

CREATE TABLE rms.resource_lock
(
   id NVARCHAR(255)  NOT NULL,
   last_updated DATETIME2(6)  NOT NULL,
   status INT  DEFAULT 0  NOT NULL,
   CONSTRAINT RESOURCE_LOCK_PKEY PRIMARY KEY(id)
);

CREATE TABLE rms.sharing_transaction
(
   id NVARCHAR(36)  NOT NULL,
   duid NVARCHAR(36)  NOT NULL,
   user_id INT  NOT NULL,
   repository_id NVARCHAR(36),
   file_path_id NVARCHAR(1000),
   file_path NVARCHAR(1000),
   device_id NVARCHAR(255),
   device_type INT  NOT NULL,
   creation_time DATETIME2(6)  NOT NULL,
   status INT  DEFAULT 0  NOT NULL,
   comments NVARCHAR(250),
   CONSTRAINT SHARING_TRANSACTION_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_SHARING_NXL FOREIGN KEY(duid)
   REFERENCES rms.all_nxl(duid)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
,
   CONSTRAINT FK_SHARING_REPOSITORY FOREIGN KEY(repository_id)
   REFERENCES rms.repository(id)  ON DELETE NO ACTION,
   CONSTRAINT FK_SHARING_USER FOREIGN KEY(user_id)
   REFERENCES rms.users(id)  ON DELETE CASCADE
);

CREATE INDEX IDX_DUID_SHARE_TXN
ON rms.sharing_transaction
(duid);

CREATE TABLE rms.sharing_recipient
(
   duid NVARCHAR(36)  NOT NULL,
   email NVARCHAR(255)  NOT NULL,
   transaction_id NVARCHAR(36)  NOT NULL,
   last_modified DATETIME2(6)  NOT NULL,
   status INT  DEFAULT 0  NOT NULL,
   CONSTRAINT SHARING_RECIPIENT_PKEY PRIMARY KEY(duid,email),
   CONSTRAINT FK_RECIPIENT_TRANSACTION FOREIGN KEY(transaction_id)
   REFERENCES rms.sharing_transaction(id)  ON DELETE CASCADE
);

CREATE INDEX IDX_DUID_SHARE_RECPT
ON rms.sharing_recipient
(duid);

CREATE TABLE rms.task_status
(
   id NVARCHAR(255)  NOT NULL,
   last_successful_update DATETIME2(6)  NOT NULL,
   last_failed_update DATETIME2(6)  NOT NULL,
   status INT  DEFAULT 0  NOT NULL,
   CONSTRAINT TASK_STATUS_PKEY PRIMARY KEY(id)
);

CREATE TABLE rms.user_preferences
(
   id INT  NOT NULL,
   expiry NVARCHAR(MAX),
   watermark NVARCHAR(255),
   preferences NVARCHAR(MAX),
   CONSTRAINT USER_PREFERENCES_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_USER_ID FOREIGN KEY(id)
   REFERENCES rms.users(id)  ON DELETE CASCADE
);

CREATE TABLE rms.user_session
(
   id BIGINT IDENTITY NOT NULL,
   client_id NVARCHAR(32)  NOT NULL,
   creation_time DATETIME2(6)  NOT NULL,
   device_type INT  NOT NULL,
   expiration_time DATETIME2(6)  NOT NULL,
   login_type INT  NOT NULL,
   status INT  NOT NULL,
   ticket VARBINARY(MAX)  NOT NULL,
   ttl BIGINT  NOT NULL,
   user_id INT  NOT NULL,
   CONSTRAINT USER_SESSION_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_USER_SESSION_USER FOREIGN KEY(user_id)
   REFERENCES rms.users(id)  
-- ON DELETE NO ACTION was removed since such behaviour is default while NO ACTION keyword(s) not supported
);

CREATE INDEX IDX_USER_SESSION_1
ON rms.user_session
(user_id, 
status, 
client_id, 
device_type);

CREATE TABLE rms.feedback
(
  id NVARCHAR(36) NOT NULL,
  type NVARCHAR(250) NOT NULL,
  summary NVARCHAR(100) NOT NULL, -- size from UI
  description NVARCHAR(2000) NOT NULL, -- size from UI
  client_id NVARCHAR(32) NOT NULL,
  device_id NVARCHAR(32),
  device_type INT NOT NULL,
  user_id INT NOT NULL,
  creation_time DATETIME2(6) NOT NULL,
  CONSTRAINT FEEDBACK_PKEY PRIMARY KEY(id)
);



