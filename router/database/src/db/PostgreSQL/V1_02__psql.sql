create table key_store_entry (
    id varchar(36) not null,
    creation_time timestamp not null,
    credential varchar(255) not null,
    data oid not null,
    key_store_type varchar(15) not null,
    tenant_name varchar(250),
    version int not null,
    primary key (id),
    constraint uk_key_store_tenant unique (tenant_name)
);