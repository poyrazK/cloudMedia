create table users (
    id varchar(36) primary key,
    email varchar(255) not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table user_credentials (
    user_id varchar(36) primary key,
    password_hash varchar(255) not null,
    updated_at timestamp not null,
    constraint fk_user_credentials_user
        foreign key (user_id) references users (id) on delete cascade
);

create table oauth_accounts (
    id varchar(36) primary key,
    user_id varchar(36) not null,
    provider varchar(32) not null,
    provider_subject varchar(255) not null,
    linked_at timestamp not null,
    constraint fk_oauth_accounts_user
        foreign key (user_id) references users (id) on delete cascade
);

create table sessions (
    id varchar(36) primary key,
    user_id varchar(36) not null,
    device_id varchar(64),
    user_agent varchar(255),
    ip_address varchar(64),
    created_at timestamp not null,
    expires_at timestamp not null,
    revoked_at timestamp,
    constraint fk_sessions_user
        foreign key (user_id) references users (id) on delete cascade
);

create table refresh_tokens (
    id varchar(36) primary key,
    session_id varchar(36) not null,
    token_hash varchar(128) not null,
    parent_token_id varchar(36),
    issued_at timestamp not null,
    expires_at timestamp not null,
    revoked_at timestamp,
    replaced_by varchar(36),
    constraint fk_refresh_tokens_session
        foreign key (session_id) references sessions (id) on delete cascade,
    constraint fk_refresh_tokens_parent
        foreign key (parent_token_id) references refresh_tokens (id) on delete set null,
    constraint fk_refresh_tokens_replaced_by
        foreign key (replaced_by) references refresh_tokens (id) on delete set null
);
