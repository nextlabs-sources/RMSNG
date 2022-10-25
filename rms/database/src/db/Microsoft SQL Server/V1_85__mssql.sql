ALTER TABLE rms.sharing_transaction ALTER COLUMN duid NVARCHAR(36) NULL;
ALTER TABLE rms.sharing_transaction DROP CONSTRAINT FK_SHARING_REPOSITORY;
ALTER TABLE rms.sharing_transaction DROP COLUMN repository_id, file_path_id, file_path ;
GO

EXEC sp_rename 'rms.sharing_transaction.duid', 'myspace_duid', 'COLUMN';
ALTER TABLE rms.sharing_transaction ADD project_duid NVARCHAR(36), ews_duid NVARCHAR(36), parent_id NVARCHAR(36), from_space SMALLINT NOT NULL DEFAULT 0;
EXEC sp_rename 'rms.sharing_recipient', 'sharing_recipient_personal';
GO

CREATE TABLE rms.sharing_recipient_project
(
    duid NVARCHAR(36) NOT NULL,
    project_id INT NOT NULL,
    transaction_id NVARCHAR(36) NOT NULL,
    last_modified DATETIME2(6) NOT NULL,
    status INT  DEFAULT 0  NOT NULL,
    CONSTRAINT SHARING_RECIPIENT_PRJ_PKEY PRIMARY KEY(duid, project_id),
    CONSTRAINT FK_RECIPIENT_PRJ_TXN FOREIGN KEY(transaction_id)
        REFERENCES rms.sharing_transaction(id)  ON DELETE CASCADE
);
GO

CREATE TABLE rms.sharing_recipient_tenant
(
    duid NVARCHAR(36)  NOT NULL,
    tenant_id NVARCHAR(36)  NOT NULL,
    transaction_id NVARCHAR(36)  NOT NULL,
    last_modified DATETIME2(6)  NOT NULL,
    status INT  DEFAULT 0  NOT NULL,
    CONSTRAINT SHARING_RECIPIENT_TENANT_PKEY PRIMARY KEY(duid, tenant_id),
    CONSTRAINT FK_RECIPIENT_TENANT_TXN FOREIGN KEY(transaction_id)
        REFERENCES rms.sharing_transaction(id)  ON DELETE CASCADE
);

CREATE INDEX IDX_PROJECT_DUID_SHARE_TXN
    ON rms.sharing_transaction
        (project_duid);

CREATE INDEX IDX_TENANT_DUID_SHARE_TXN
    ON rms.sharing_transaction
        (ews_duid);

CREATE INDEX IDX_PROJECT_DUID_SHARE_RECPT
    ON rms.sharing_recipient_project
        (duid);

CREATE INDEX IDX_TENANT_DUID_SHARE_RECPT
    ON rms.sharing_recipient_tenant
        (duid);
