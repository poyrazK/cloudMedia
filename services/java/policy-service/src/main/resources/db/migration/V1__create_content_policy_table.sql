CREATE TABLE content_policy (
    content_id VARCHAR(36) PRIMARY KEY,
    age_restricted BOOLEAN NOT NULL DEFAULT FALSE,
    geo_allow_list VARCHAR(512) NOT NULL DEFAULT '',
    geo_block_list VARCHAR(512) NOT NULL DEFAULT '',
    moderation_state VARCHAR(16) NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_content_policy_moderation_state ON content_policy(moderation_state);
