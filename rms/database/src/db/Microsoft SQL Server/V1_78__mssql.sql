CREATE TABLE rms.nxl_metadata
(
    duid NVARCHAR(36) NOT NULL,
    owner NVARCHAR(150) NOT NULL,
    file_policy_checksum NVARCHAR(256) NOT NULL,
    file_tags_checksum NVARCHAR(256) NOT NULL,
    protection_type int NOT NULL,
    tenant_id NVARCHAR(36),
    status int NOT NULL,
    otp NVARCHAR(36),
    creation_time DATETIME2(6) NOT NULL,
    last_modified DATETIME2(6) NOT NULL,
    CONSTRAINT nxl_metadata_pkey PRIMARY KEY (duid)
);