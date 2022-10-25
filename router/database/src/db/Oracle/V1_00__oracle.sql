CREATE TABLE client
(
   client_id VARCHAR2(32) NOT NULL,
   device_id VARCHAR2(50) NOT NULL,
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

CREATE TABLE crash
(
   hash VARCHAR2(32) NOT NULL,
   client_id VARCHAR2(32),
   stacktrace CLOB,
   CONSTRAINT crash_pkey PRIMARY KEY(hash)
);

CREATE TABLE crash_log
(
   id NUMBER(10,0) NOT NULL,
   hash VARCHAR2(32) NOT NULL,
   "log" CLOB,
   creation_date TIMESTAMP(6) NOT NULL,
   CONSTRAINT crash_log_pkey PRIMARY KEY(id)
);

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

CREATE TABLE tenant
(
   id VARCHAR2(36) NOT NULL,
   name VARCHAR2(250),
   otp BLOB,
   hsk BLOB,
   server VARCHAR2(150),
   display_name VARCHAR2(150),
   description VARCHAR2(2000),
   email VARCHAR2(250),
   creation_time TIMESTAMP(6) NOT NULL,
   CONSTRAINT tenant_pkey PRIMARY KEY(id),
   CONSTRAINT u_tenant_name UNIQUE(name)
);

CREATE SEQUENCE  "CSN"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 21 CACHE 20 NOORDER  NOCYCLE ;
CREATE SEQUENCE  "CRASH_LOG_ID_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 NOCACHE ;