# CloudMedia Kafka Event Catalog (MVP)

This document defines topic ownership, event envelope, partition keys, and core payload schemas.

## 1) Envelope contract

All events must use this envelope:

```json
{
  "event_id": "evt_01H...",
  "event_type": "content.published",
  "event_version": 1,
  "occurred_at": "2026-03-09T12:00:00Z",
  "producer": "content-service",
  "entity_type": "content",
  "entity_id": "cnt_123",
  "trace_id": "req_abc",
  "payload": {}
}
```

## 2) Topic and key strategy

- Topic namespace: `cloudmedia.<domain>.<event>`
- Current content-service producer topics:
  - `cloudmedia.content.created`
  - `cloudmedia.content.updated`
  - `cloudmedia.content.published`
  - `cloudmedia.content.unpublished`
- Partition keys:
  - content events: `content_id`
  - user events: `user_id`
  - live events: `stream_id`
- Delivery semantics: at-least-once
- Consumer requirement: idempotent handling
- Each consumer group must define a DLQ topic

## 3) Event list

### Identity
- `user.created`
- `user.updated`
- `user.verified`

### Upload and content lifecycle
- `upload.completed`
- `content.created`
- `content.ready`
- `content.published`
- `content.updated`
- `content.unpublished`

### Policy and moderation
- `policy.changed`
- `content.moderation.changed`

### Social
- `follow.created`
- `comment.created`
- `comment.reported`
- `playlist.updated`

### Livestream and replay
- `live.started`
- `live.ended`
- `live.replay.requested`
- `live.replay.ready`

### Search/index and engagement
- `index.upsert.requested`
- `index.delete.requested`
- `engagement.updated`

## 4) Core payload schemas

### `upload.completed` payload

```json
{
  "upload_session_id": "upl_123",
  "uploader_user_id": "usr_123",
  "storage_uri": "object://uploads/raw/file.mp4",
  "content_type": "video/mp4",
  "size_bytes": 104857600
}
```

### `content.published` payload

```json
{
  "content_id": "cnt_123",
  "channel_id": "chn_123",
  "title": "My video",
  "description": "...",
  "tags": ["travel", "vlog"],
  "category": "lifestyle",
  "duration_seconds": 640,
  "published_at": "2026-03-09T12:00:00Z",
  "policy": {
    "age_restricted": false,
    "geo_allow": [],
    "geo_block": []
  }
}
```

### `content.created` payload

```json
{
  "content_id": "cnt_123",
  "channel_id": "chn_123",
  "title": "My draft",
  "content_type": "VIDEO",
  "visibility": "PRIVATE",
  "state": "DRAFT"
}
```

### `content.updated` payload

```json
{
  "content_id": "cnt_123",
  "channel_id": "chn_123",
  "title": "Updated title",
  "content_type": "VIDEO",
  "visibility": "UNLISTED"
}
```

### `content.unpublished` payload

```json
{
  "content_id": "cnt_123",
  "channel_id": "chn_123",
  "previous_state": "PUBLISHED",
  "current_state": "PRIVATE",
  "published_at": "2026-03-09T12:00:00Z"
}
```

### `policy.changed` payload

```json
{
  "content_id": "cnt_123",
  "age_restricted": true,
  "geo_allow": ["TR"],
  "geo_block": [],
  "moderation_state": "visible",
  "updated_by": "mod_123"
}
```

### `live.replay.ready` payload

```json
{
  "stream_id": "str_123",
  "replay_content_id": "cnt_999",
  "visibility": "public",
  "ready_at": "2026-03-09T13:00:00Z"
}
```

## 5) Versioning rules

- Backward-compatible additions: bump minor schema docs, keep `event_version` same.
- Breaking changes: increment `event_version`, publish migration note, dual-consume during transition.
- Event type names are immutable once accepted.

## 6) Operational constraints

- Maximum payload size must stay below broker limits and team policy.
- Large blobs are forbidden in payloads; reference by IDs/URIs.
- Consumers emit processing metrics and DLQ counters.
