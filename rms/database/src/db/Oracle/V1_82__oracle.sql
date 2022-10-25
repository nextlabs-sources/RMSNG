CREATE TABLE api_user_cert
(
   id VARCHAR2(36) NOT NULL,
   api_user_id NUMBER(10,0) NOT NULL,
   data BLOB NOT NULL,
   cert_alias VARCHAR2(250),
   creation_time TIMESTAMP(6) NOT NULL,
   last_modified TIMESTAMP(6) NOT NULL,
   CONSTRAINT api_user_cert_pkey PRIMARY KEY(id),
   CONSTRAINT fk_api_user_id FOREIGN KEY(api_user_id)
   REFERENCES users(id) ON DELETE CASCADE,
   CONSTRAINT u_api_user_cert_alias UNIQUE(api_user_id, cert_alias)
);