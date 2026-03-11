create table channels (
    id varchar(36) primary key,
    slug varchar(128) not null,
    display_name varchar(255) not null,
    description varchar(2000),
    created_at timestamp not null,
    updated_at timestamp not null
);

create table channel_members (
    id varchar(36) primary key,
    channel_id varchar(36) not null,
    user_id varchar(36) not null,
    role varchar(32) not null,
    joined_at timestamp not null,
    constraint fk_channel_members_channel
        foreign key (channel_id) references channels (id) on delete cascade
);

create table content (
    id varchar(36) primary key,
    channel_id varchar(36) not null,
    title varchar(255) not null,
    description varchar(4000),
    content_type varchar(32) not null,
    state varchar(32) not null,
    visibility varchar(32) not null,
    playback_ready boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    published_at timestamp,
    constraint fk_content_channel
        foreign key (channel_id) references channels (id) on delete cascade
);
