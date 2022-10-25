CREATE TABLE nxl_metadata
(
    duid character varying(36) NOT NULL,
    owner character varying(150) NOT NULL,
    file_policy_checksum character varying(256) NOT NULL,
    file_tags_checksum character varying(256) NOT NULL,
    protection_type integer NOT NULL,
    tenant_id character varying(36),
    status integer NOT NULL,
    otp character varying(36),
    creation_time timestamp WITH TIME ZONE NOT NULL,
    last_modified timestamp WITH TIME ZONE NOT NULL,
    PRIMARY KEY (duid)
);