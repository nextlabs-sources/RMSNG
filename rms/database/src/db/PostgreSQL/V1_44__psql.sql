CREATE TABLE user_session (
    id bigserial not null,
    client_id varchar(32) not null,
    creation_time timestamp not null,
    device_type int not null,
    expiration_time timestamp not null,
    login_type int not null,
    status int not null,
    ticket bytea not null,
    ttl bigint not null,
    user_id int not null,
    PRIMARY KEY (id)
);
CREATE INDEX idx_user_session_1 ON user_session (user_id, status, client_id, device_type);
ALTER TABLE user_session ADD CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES "user";

ALTER TABLE "user" DROP COLUMN ttl;
ALTER TABLE "user" DROP COLUMN ticket;