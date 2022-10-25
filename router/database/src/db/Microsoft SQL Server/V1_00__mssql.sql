CREATE TABLE router.client
(
   client_id NVARCHAR(32)  NOT NULL,
   device_id NVARCHAR(50)  NOT NULL,
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

CREATE TABLE router.crash
(
   hash NVARCHAR(32)  NOT NULL,
   client_id NVARCHAR(32),
   stacktrace NVARCHAR(MAX),
   CONSTRAINT CRASH_PKEY PRIMARY KEY(hash)
);

CREATE TABLE router.crash_log
(
   id INT  NOT NULL,
   hash NVARCHAR(32)  NOT NULL,
   log NVARCHAR(MAX),
   creation_date DATETIME2(6)  NOT NULL,
   CONSTRAINT CRASH_LOG_PKEY PRIMARY KEY(id)
);

CREATE TABLE router.key_store_entry
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

CREATE TABLE router.tenant
(
   id NVARCHAR(36)  NOT NULL,
   name NVARCHAR(250),
   otp VARBINARY(MAX),
   hsk VARBINARY(MAX),
   server NVARCHAR(150),
   display_name NVARCHAR(150),
   description NVARCHAR(2000),
   email NVARCHAR(250),
   creation_time DATETIME2(6)  NOT NULL,
   CONSTRAINT TENANT_PKEY PRIMARY KEY(id),
   CONSTRAINT U_TENANT_NAME UNIQUE(name)
);





