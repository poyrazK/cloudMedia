CREATE TABLE content_policy (
    content_id VARCHAR(36) PRIMARY KEY,
    age_restricted BOOLEAN NOT NULL DEFAULT FALSE,
    geo_allow_list VARCHAR(512) NOT NULL DEFAULT '',
    geo_block_list VARCHAR(512) NOT NULL DEFAULT '',
    moderation_state VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
