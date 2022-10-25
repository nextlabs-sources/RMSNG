ALTER TABLE sharing_transaction ALTER COLUMN duid DROP NOT NULL;
ALTER TABLE sharing_transaction DROP COLUMN  repository_id, DROP COLUMN file_path_id, DROP COLUMN file_path;
ALTER TABLE sharing_transaction RENAME duid TO myspace_duid;
ALTER TABLE sharing_transaction ADD COLUMN project_duid VARCHAR(36), ADD COLUMN ews_duid VARCHAR(36), ADD COLUMN parent_id VARCHAR(36), ADD COLUMN from_space SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE sharing_recipient RENAME TO sharing_recipient_personal;

CREATE table sharing_recipient_project (
   duid character varying(36) NOT NULL,
   project_id integer NOT NULL,
   transaction_id character varying(36) NOT NULL,
   last_modified timestamp with time zone NOT NULL,
   status smallint NOT NULL DEFAULT 0,
   CONSTRAINT sharing_recipient_prj_pkey PRIMARY KEY (duid , project_id),
   CONSTRAINT fk_recipient_prj_txn FOREIGN KEY (transaction_id)
       REFERENCES sharing_transaction (id) ON DELETE CASCADE
);

CREATE TABLE sharing_recipient_tenant
(
    duid character varying(36) NOT NULL,
    tenant_id character varying(36) NOT NULL,
    transaction_id character varying(36) NOT NULL,
    last_modified timestamp with time zone NOT NULL,
    status smallint NOT NULL DEFAULT 0,
    CONSTRAINT sharing_recipient_tenant_pkey PRIMARY KEY (duid , tenant_id),
    CONSTRAINT fk_recipient_tenant_txn FOREIGN KEY (transaction_id)
        REFERENCES sharing_transaction (id) ON DELETE CASCADE
);

CREATE INDEX idx_project_duid_share_txn ON sharing_transaction(project_duid);
CREATE INDEX idx_tenant_duid_share_txn ON sharing_transaction(ews_duid);
CREATE INDEX idx_project_duid_share_recpt ON sharing_recipient_project(duid);
CREATE INDEX idx_tenant_duid_share_recpt ON sharing_recipient_tenant(duid);