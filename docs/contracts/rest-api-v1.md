# CloudMedia REST API v1 Contract

This document defines the MVP API contract groups, standards, and core request/response shapes.

## 1) Global standards

- Base path: `/v1`
- Authentication: `Authorization: Bearer <access_token>` for protected routes
- Idempotency: `Idempotency-Key` required on critical writes
- Correlation: `X-Request-Id` accepted from caller; generated if missing
- Pagination: cursor-based for feed/search/comment lists

### 1.1 Success response shape

```json
{
  "data": {},
  "meta": {
    "request_id": "req_123",
    "timestamp": "2026-03-09T12:00:00Z"
  }
}
```

### 1.2 Error response shape

```json
{
  "error": {
    "code": "AUTH_TOKEN_EXPIRED",
    "message": "Access token expired",
    "details": {}
  },
  "meta": {
    "request_id": "req_123",
    "timestamp": "2026-03-09T12:00:00Z"
  }
}
```

## 2) Auth and identity

### `POST /v1/auth/login`
- Request: email/password
- Response: access token + refresh token + session id

### `POST /v1/auth/social-login`
- Request: provider, provider_token, optional device info
- MVP provider support: `GOOGLE` only
- Current implementation uses a fake verifier for development/testing only.
- Fake token format: `fake-google:<subject>:<email>`
- Response: access token + refresh token + session id

### `POST /v1/auth/refresh`
- Request: refresh token
- Response: new access token + new refresh token (rotation)

### `POST /v1/auth/logout`
- Request: current session or all sessions flag
- Response: success status

### 2.1 Identity MVP implementation notes
- Access token TTL: `15 minutes`
- Refresh token TTL: `30 days`
- Refresh strategy: rotation on every refresh request
- Reuse detection: revoke refresh token family + owning session
- Session cap: max `5` active sessions per user (oldest revoked first)

## 3) Upload and content

### `POST /v1/uploads/sessions`
- Creates resumable upload session
- Validates creator quota and content type

### `POST /v1/uploads/sessions/{session_id}/finalize`
- Marks upload complete and emits `upload.completed`

### `POST /v1/content`
- Creates content metadata record in draft state
- Request fields (MVP): `userId`, `channelId`, `title`, `description`, `contentType`, optional `visibility`
- Default behavior: `state=DRAFT`, `playbackReady=false`, `publishedAt=null`, `visibility=PRIVATE` when omitted

### `PATCH /v1/content/{content_id}`
- Partially updates content metadata for channel members
- Mutable fields (MVP): `title`, `description`, `visibility`

### `POST /v1/content/{content_id}/publish`
- Idempotent publish request
- Enforces moderation/policy preconditions

### `GET /v1/content/{content_id}/playback`
- Returns signed manifest URL and available renditions
- Applies age/geo/moderation checks
- Query params (MVP): optional `countryCode` (2-letter code), optional `ageVerified` (`true|false`)
- Returns `403 CONTENT_POLICY_DENIED` when policy blocks playback

## 4) Social

### `POST /v1/social/follows/{channel_id}`
- Follow channel

### `DELETE /v1/social/follows/{channel_id}`
- Unfollow channel

### `POST /v1/social/comments`
- Creates comment (blocked-word filter at write-time)

### `PATCH /v1/social/comments/{comment_id}`
- Edits comment and stores revision record

### `POST /v1/social/comments/{comment_id}/reports`
- Files moderation report

### `POST /v1/social/playlists`
- Creates playlist

## 5) Discovery and search

### `GET /v1/search`
- Keyword search over OpenSearch-backed index
- MVP exception to the global cursor-pagination rule: uses `q`, `page`, `size`
- Current semantics: `page` is 0-based, `size` max is `100`
- Supports optional policy context params: `countryCode` (ISO 3166-1 alpha-2 uppercase, e.g. `US`) and `ageVerified`
- Filters out policy-blocked content from results
- Current MVP does not yet support category, duration, or upload-time filters
- TODO: migrate search results to cursor-based pagination after the initial read API stabilizes

### `GET /v1/search/autocomplete`
- Title suggestion endpoint over the derived search index
- MVP params: required non-blank `q`; optional integer `size` with min `1`, default `5`, max `10`

### `GET /v1/discovery/home`
- Balanced feed (followed + trending + fresh + similar)
- MVP params: optional `userId`; optional integer `size` with min `1`, default `20`, max `50`
- Returns a generic blended feed when `userId` is absent

### `GET /v1/discovery/trending`
- Region-level trending feed

## 6) Livestream and chat

### `POST /v1/live/streams`
- Creates stream with ingest credentials

### `POST /v1/live/streams/{stream_id}/start`
- Starts stream session

### `POST /v1/live/streams/{stream_id}/end`
- Ends stream and triggers replay job

### `GET /v1/live/streams/{stream_id}/replay-status`
- Returns replay processing status and resulting content id

### `POST /v1/chat/rooms/{room_id}/messages`
- Sends chat message
- Applies anti-spam and moderation hooks

## 7) Policy and moderation

### `PATCH /v1/policy/content/{content_id}`
- Updates age restriction, geo allow/block rules
- Request supports partial upsert of `ageRestricted`, `geoAllowList`, and `geoBlockList`
- Omitted fields remain unchanged; empty geo lists clear that specific list

### `PATCH /v1/moderation/content/{content_id}`
- Applies moderation state (visible, hidden, removed)
- Request supports `moderationState` with values `VISIBLE`, `HIDDEN`, or `REMOVED`
- Reuses the content policy record and preserves existing age/geo fields

### `POST /v1/policy/content/{content_id}/evaluate`
- Evaluates whether content is allowed for viewing/playback in a given request context
- Request supports optional `countryCode` and optional `ageVerified`

### `GET /v1/moderation/comments/reports`
- Lists reported comments for moderator queue

## 8) HTTP status usage

- `200`: success
- `201`: resource created
- `202`: async accepted
- `400`: validation error
- `401`: auth required/invalid token
- `403`: access denied (policy or role)
- `404`: resource not found
- `409`: conflict/idempotency collision
- `429`: rate limit exceeded
- `500`: internal error
