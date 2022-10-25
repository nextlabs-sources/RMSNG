create table customer_account (
    id BIGSERIAL not null,
    account_type varchar(50) not null,
    creation_time timestamp not null,
    last_updated_time timestamp,
    payment_customer_id varchar(255),
    user_id int not null,
    primary key (id),
    CONSTRAINT fk_customer_account_user FOREIGN KEY (user_id) REFERENCES "user"(id)
);
    
create table payment_method (
    id BIGSERIAL not null,
    payment_customer_id varchar(255),
    status int not null,
    token varchar(255) not null,
    primary key (id)
);

create table subscription (
    id BIGSERIAL not null,
    billing_cycle_length int not null,
    billing_date timestamp,
    billing_status int not null,
    no_of_billing_cycle int,
    subscription_id varchar(100) not null,
    trial_period date,
    payment_method_id bigint,
    primary key (id),
    unique (payment_method_id),
    CONSTRAINT fk_subscription_payment_method FOREIGN KEY (payment_method_id) REFERENCES "payment_method"(id)
);

ALTER TABLE project ADD COLUMN subscription_id bigint;
ALTER TABLE project ADD CONSTRAINT fk_project_subscription FOREIGN KEY (subscription_id) REFERENCES "subscription"(id);
