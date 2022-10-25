CREATE TABLE rms.api_user_cert
(
   id NVARCHAR(36) NOT NULL,
   api_user_id INT NOT NULL,
   data VARBINARY(MAX)  NOT NULL,
   cert_alias NVARCHAR(250),
   creation_time DATETIME2(6) NOT NULL,
   last_modified DATETIME2(6) NOT NULL,
   CONSTRAINT API_USER_CERT_PKEY PRIMARY KEY(id),
   CONSTRAINT FK_API_USER_ID FOREIGN KEY(api_user_id)
   REFERENCES rms.users(id) ON DELETE CASCADE,
   CONSTRAINT U_API_USER_CERT_ALIAS UNIQUE(api_user_id, cert_alias)
);