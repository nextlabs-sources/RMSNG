CREATE TABLE nxl_metadata
(
    duid VARCHAR2(36) NOT NULL,
    owner VARCHAR2(150) NOT NULL,
    file_policy_checksum VARCHAR2(256) NOT NULL,
    file_tags_checksum VARCHAR2(256) NOT NULL,
    protection_type NUMBER(10,0) NOT NULL,
    tenant_id VARCHAR2(36),
    status NUMBER(10,0) NOT NULL,
    otp VARCHAR2(36),
    creation_time TIMESTAMP(6) NOT NULL,
    last_modified TIMESTAMP(6) NOT NULL,
    CONSTRAINT nxl_metadata_pkey PRIMARY KEY (duid)
);