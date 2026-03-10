alter table refresh_tokens
    add column family_id varchar(36);

update refresh_tokens
set family_id = id
where family_id is null;

alter table refresh_tokens
    alter column family_id set not null;

create index ix_refresh_tokens_family_revoked on refresh_tokens (family_id, revoked_at);
