ALTER TABLE sharing_transaction MODIFY duid VARCHAR2(36) NULL;
ALTER TABLE sharing_transaction DROP (repository_id, file_path_id, file_path);
ALTER TABLE sharing_transaction RENAME COLUMN duid TO myspace_duid;
ALTER TABLE sharing_transaction ADD (project_duid VARCHAR2(36), ews_duid VARCHAR2(36), parent_id VARCHAR(36), from_space NUMBER(5,0) DEFAULT 0 NOT NULL);
ALTER table sharing_recipient RENAME TO sharing_recipient_personal;

CREATE TABLE sharing_recipient_project
(
    duid VARCHAR2(36) NOT NULL,
    project_id NUMBER(10,0) NOT NULL,
    transaction_id VARCHAR2(36) NOT NULL,
    last_modified TIMESTAMP(6) NOT NULL,
    status NUMBER(5,0)  DEFAULT 0 NOT NULL,
    CONSTRAINT sharing_recipient_prj_pkey PRIMARY KEY(duid, project_id),
    CONSTRAINT fk_recipient_prj_txn FOREIGN KEY(transaction_id)
        REFERENCES sharing_transaction(id)  ON DELETE CASCADE
);

CREATE TABLE sharing_recipient_tenant
(
    duid VARCHAR2(36) NOT NULL,
    tenant_id VARCHAR2(36) NOT NULL,
    transaction_id VARCHAR2(36) NOT NULL,
    last_modified TIMESTAMP(6) NOT NULL,
    status NUMBER(5,0)  DEFAULT 0 NOT NULL,
    CONSTRAINT sharing_recipient_tenant_pkey PRIMARY KEY(duid, tenant_id),
    CONSTRAINT fk_recipient_tenant_txn FOREIGN KEY(transaction_id)
        REFERENCES sharing_transaction(id)  ON DELETE CASCADE
);

CREATE INDEX idx_project_duid_share_txn
    ON sharing_transaction
        (project_duid);

CREATE INDEX idx_ews_duid_share_txn
    ON sharing_transaction
        (ews_duid);

CREATE INDEX idx_project_duid_share_recpt
    ON sharing_recipient_project
        (duid);

CREATE INDEX idx_tenant_duid_share_recpt
    ON sharing_recipient_tenant
        (duid);