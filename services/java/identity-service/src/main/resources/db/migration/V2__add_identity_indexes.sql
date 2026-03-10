create unique index ux_users_email on users (email);

create unique index ux_oauth_provider_subject on oauth_accounts (provider, provider_subject);

create index ix_sessions_user_id on sessions (user_id);
create index ix_sessions_user_revoked on sessions (user_id, revoked_at);

create unique index ux_refresh_token_hash on refresh_tokens (token_hash);
create index ix_refresh_tokens_session_id on refresh_tokens (session_id);
create index ix_refresh_tokens_session_revoked on refresh_tokens (session_id, revoked_at);
