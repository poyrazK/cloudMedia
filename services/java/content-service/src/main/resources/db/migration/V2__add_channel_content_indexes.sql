create unique index ux_channels_slug on channels (slug);

create unique index ux_channel_members_channel_user on channel_members (channel_id, user_id);
create index ix_channel_members_user_id on channel_members (user_id);
create index ix_channel_members_channel_id on channel_members (channel_id);

create index ix_content_channel_id on content (channel_id);
create index ix_content_state on content (state);
create index ix_content_visibility on content (visibility);
create index ix_content_published_at on content (published_at);
