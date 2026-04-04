# CloudMedia MVP Backend Implementation Plan

## 1) Objective

Build a cloud-hosted UGC media platform backend (YouTube-like) with:
- Video + music metadata hosting
- VOD upload/transcoding/playback (1080p + 4K)
- Livestream MVP + basic chat + automatic replay-to-VOD
- Discovery/search with OpenSearch
- Social features: comments, follows, playlists
- Manual moderation with basic automated safeguards
- MVP policy controls: age restriction + geo-blocking

Frontend is out of scope for this document.

---

## 2) Locked Decisions

- API style: **REST-first**
- Core stack: **Java (Spring Boot) + Go**
- Event backbone: **Kafka**
- Search: **OpenSearch** (integrated in MVP, no separate search engine project now)
- Auth: **Email + social login**
- Token strategy: **JWT access token + refresh token rotation**
- Discovery: **Balanced approach** (personalized + trending)
- Livestream: **MVP supported**
- Chat: **Basic chat**, retention **7 days**
- Replay: **Optional visibility**, default **public**
- Comments: **Editable**
- Comment moderation: **Option 2** (blocked-word filter + report queue + manual review)
- Resolution access: **All users can access available renditions including 4K**
- Music rights: **Rights-ready schema only** (no full rights workflow in MVP)

---

## 3) Architecture Overview

### 3.1 Logical Architecture
- API gateway routes requests and enforces cross-cutting controls.
- Domain services own business logic and data boundaries.
- Media pipeline services handle upload, transcode, packaging, and live control plane.
- Kafka coordinates async workflows and index updates.
- OpenSearch serves search/discovery retrieval needs.
- Postgres remains system of record.

### 3.2 Data Systems
- **Postgres**: transactional source of truth
- **OpenSearch**: search/discovery read index
- **Redis**: cache, rate limits, ephemeral state (chat/session acceleration)
- **Object storage**: source media, renditions, manifests, thumbnails
- **Kafka**: events and async orchestration
- **Background workers**: transcode orchestration, indexing consumers, retention/purge jobs

---

## 4) Service Boundaries & Ownership

## 4.1 Java (Spring Boot) Services
1. **identity-service**
   - Email/social auth, account linking, session management, token issuance
2. **content-service**
   - Content metadata, publish lifecycle, ownership, media asset records
3. **social-service**
   - Comments, follows, playlists, report workflows
4. **policy-service**
   - Age gate, geo-blocking, moderation states
5. **discovery-service**
   - Candidate aggregation and balanced ranking

## 4.2 Go Services
1. **api-gateway**
   - Authn/authz checks, routing, rate limits, request correlation
2. **upload-service**
   - Resumable upload sessions, validation, quotas
3. **media-control-service**
   - Transcode/package workflow orchestration
4. **livestream-service**
   - Stream lifecycle, ingest keys, replay trigger
5. **chat-service**
   - Live room messaging, anti-spam, moderation hooks
6. **worker-runtime**
   - Async processors (thumbnail tasks, retries, purges, index sync support if needed)

**Boundary rule:** No cross-service direct DB access.

---

## 5) Core Data Model Domains

### 5.1 Identity
- users
- user_credentials
- oauth_accounts
- sessions
- refresh_tokens
- device_fingerprints (optional MVP+)

### 5.2 Content
- channels
- assets
- media_versions
- renditions
- thumbnails
- publish_states

### 5.3 Social
- follows
- comments
- comment_revisions
- comment_reports
- playlists
- playlist_items

### 5.4 Policy & Moderation
- content_policy_flags (age_gate, geo policies)
- moderation_actions
- moderation_audit_logs

### 5.5 Live & Chat
- live_streams
- stream_keys
- live_sessions
- live_replay_jobs
- chat_rooms
- chat_messages (7-day retention)

### 5.6 Rights-Ready Fields (MVP metadata only)
- rights_owner_type
- rights_status
- territory_policy
- license_start_at
- license_end_at
- claim_status
- source_provenance

---

## 6) API Surface (REST v1)

- `/v1/auth/*` register/login/social-login/refresh/logout/session revoke
- `/v1/users/*` profile, verification status
- `/v1/channels/*` channel CRUD
- `/v1/uploads/*` create session, commit part, finalize
- `/v1/content/*` create/update/publish/unpublish/get/playback
- `/v1/social/*` follow/unfollow, comments, reports, playlists
- `/v1/policy/*` age/geo flags, moderation updates
- `/v1/search/*` query + autocomplete (OpenSearch-backed)
- `/v1/discovery/*` home, trending, followed, related
- `/v1/live/*` create/start/end/status/replay status
- `/v1/chat/*` join/send/mod actions
- `/v1/moderation/*` queue/list/action/audit

Standards:
- Cursor pagination for feed/comment/search lists
- Idempotency keys for critical writes (publish/start/stop)
- Unified error envelope and trace ID on every response
- Versioned APIs (`/v1`) and schema evolution policy

---

## 7) Event-Driven Contract (Kafka)

### 7.1 Topic/Event Set
- Identity: `user.created`, `user.updated`, `user.verified`
- Upload/Content: `upload.completed`, `content.created`, `content.ready`, `content.published`, `content.updated`, `content.unpublished`
- Policy/Moderation: `policy.changed`, `content.moderation.changed`
- Social: `follow.created`, `comment.created`, `comment.reported`, `playlist.updated`
- Live: `live.started`, `live.ended`, `live.replay.requested`, `live.replay.ready`
- Search/Index: `index.upsert.requested`, `index.delete.requested`
- Engagement: `engagement.updated`

### 7.2 Event Envelope (recommended)
- `event_id`
- `event_type`
- `event_version`
- `occurred_at`
- `producer`
- `entity_type`
- `entity_id`
- `payload`
- `trace_id`

---

## 8) Search & Discovery (MVP)

## 8.1 OpenSearch Role
- Index content metadata + lightweight engagement + policy flags
- Serve keyword search, autocomplete, and retrieval for discovery candidates
- Remain derived/secondary (Postgres is source of truth)

## 8.2 Indexing Triggers
- On publish/update/unpublish
- On policy change
- On creator/profile updates affecting search display
- On periodic engagement refresh

## 8.3 Balanced Discovery Strategy
- Candidate pools:
  - Followed creators
  - Trending
  - Fresh uploads
  - Similar tags/categories
- Ranking signals:
  - Watch-time
  - Completion rate
  - Freshness
  - Like/follow engagement
- Guardrails:
  - Creator diversity cap
  - New-content exploration quota
  - Policy/moderation filtering

---

## 9) Media Pipelines

## 9.1 VOD Pipeline
1. Create upload session
2. Resumable upload to object storage
3. Finalize upload
4. Emit `upload.completed`
5. Transcode + package HLS (1080p + 4K where possible)
6. Generate thumbnails/previews
7. Emit `content.ready`
8. Publish (manual/automatic based on workflow)

## 9.2 Livestream Pipeline
1. Create live stream and issue ingest key
2. Start stream session and health monitoring
3. Route to packaging/manifest generation
4. Enable chat room
5. On end: emit `live.ended` and trigger replay job
6. Replay is processed and published as VOD (default visibility public, editable)

---

## 10) Security, Auth, and Access Control

- Access JWT TTL: **15 minutes**
- Refresh token TTL: **30 days**
- Refresh token rotation with reuse detection
- Session/device cap: **5 active sessions per user**
- Global logout and per-session revoke support
- Signed playback URLs for media access
- RBAC baseline: user / creator / moderator / admin / service
- Policy checks at playback/search/discovery layers (age/geo/mod state)

---

## 11) Moderation & Policy Rules (MVP)

### 11.1 Comments
- Write-time blocked-word filter
- User report flow
- Moderator queue with actions:
  - delete comment
  - mute user
  - temp/permanent comment ban
- Comments editable by author
- `comment_revisions` retained for moderation/audit (recommended: 30 days)

### 11.2 Content Policy
- Age restriction optional per content
- Geo-block/allow optional per content
- Admin override for takedown/hide

---

## 12) Operational Requirements

- Rate limits by endpoint/user role/content type
- Retry + dead-letter queues for failed async processing
- 7-day chat retention purge job
- Metrics/tracing/logging baseline:
  - API latency/error rates
  - Kafka consumer lag
  - Upload finalize success rate
  - Transcode queue time and completion time
  - Live stream uptime and failure causes
  - Search index freshness lag
- Backup and restore procedures for Postgres and critical metadata

---

## 13) Implementation Order (Suggested)

1. **Foundation**
   - API conventions, auth/token model, Kafka event envelope
2. **Core VOD**
   - Upload, transcode orchestration, packaging, playback signing
3. **Policy**
   - Age/geo/mod checks integrated into playback/search/discovery
4. **Social**
   - Comments/follows/playlists + reporting + moderation queue
5. **Search**
   - OpenSearch indexing and `/search` endpoints
6. **Discovery**
   - Balanced ranking and feed APIs
7. **Live + Chat**
   - Stream lifecycle, basic chat, replay automation
8. **Hardening**
   - Rate limits, retries, observability, retention jobs, SLO tuning

---

## 14) Risks & Mitigations

- **Two-language complexity (Java + Go)**  
  Mitigation: strict ownership matrix and API/event contracts.
- **Search index drift**  
  Mitigation: idempotent upserts, replayable indexing topics, reconciliation jobs.
- **Livestream operational instability**  
  Mitigation: clear failure states, robust health checks, replay fallback handling.
- **Moderation backlog growth**  
  Mitigation: blocked-word filters, rate limits, queue prioritization.
- **4K cost/performance pressure**  
  Mitigation: transcode quotas, async scheduling, per-tier operational controls later.

---

## 15) Definition of Done (MVP Backend)

- Auth works with email + social and secure token rotation
- Users can upload and publish media; VOD playback works with 1080p/4K renditions
- Livestream start/end functions; replay is auto-generated and published
- Search and discovery are functional and policy-aware
- Comments/follows/playlists work with moderation/reporting
- Age restriction + geo-blocking enforced in playback/search/discovery
- Observability, retries, and retention jobs are operational
- No cross-service DB coupling violations

---

## 16) v2 Addendum: Reliability, Scale, and Operations

This section captures additional architectural decisions needed before production launch.

### 16.1 Non-Functional Requirements (SLO/SLI)
- API availability: **99.9%** monthly for public read endpoints
- API latency: **p95 < 300ms** for metadata/search/feed reads (excluding media delivery)
- Upload finalize success: **>= 99.5%** (excluding client network aborts)
- Publish-to-search freshness: **p95 < 60s**, **p99 < 5m**
- Livestream start success: **>= 99.0%**
- Chat message accept latency: **p95 < 250ms**
- Replay generation success: **>= 98.5%** within target processing window

### 16.2 Capacity Baseline and Scale Triggers
- Initial planning baseline:
  - Peak concurrent viewers: **300-800**
  - Concurrent livestreams: **10-30**
  - Daily uploads: **200-800 assets/day**
- Transcode queue policy under load:
  - Keep 1080p high-priority for playback readiness
  - Defer 4K completion when backlog exceeds threshold
- Scale review triggers:
  - Kafka consumer lag > 5 minutes sustained
  - Upload finalize p95 > 5 seconds sustained
  - Search freshness p95 > 60 seconds sustained
  - Live start failure > 1% daily

### 16.3 Failure Modes and Fallbacks
- Upload service degraded:
  - Return retryable errors and preserve resumable sessions
- Transcode backlog spike:
  - Prioritize 1080p publishability, complete 4K asynchronously
- OpenSearch lag/outage:
  - Degrade to limited discovery mode (trending cache + followed creators)
- Kafka consumer failures:
  - Route to DLQ, replay with idempotent consumers
- Chat overload:
  - Tighten rate limits and enable slow mode fallback
- Token reuse anomaly:
  - Revoke refresh token family and require re-authentication

### 16.4 Data Governance and Lifecycle
- Chat retention: **7 days** with scheduled purge job
- Comment revisions retention: **30 days** for moderation/audit
- Moderation audit logs retention: **>= 1 year**
- Deletion strategy:
  - Soft-delete first for operational safety
  - Scheduled hard-delete lifecycle jobs
- Add user data deletion workflow for compliance readiness

### 16.5 Security Hardening Decisions
- Internal service authentication: signed service tokens (mTLS optional when ready)
- Secret rotation cadence: at most every **90 days**
- JWT signing key rotation: documented with overlap window
- Playback authorization: short-lived signed media URLs only
- Abuse controls:
  - Per-IP and per-user rate limits
  - Report-threshold based temporary comment cooldowns

### 16.6 OpenSearch Operations Decision
- Use one primary `content` index for MVP
- Use aliases (`content_read`, `content_write`) for zero-downtime reindex
- Run nightly reconciliation from Postgres published assets to OpenSearch index

### 16.7 Kafka Contract Policy
- Event versioning required (`event_version`)
- Partition keys:
  - Content events by `content_id`
  - User events by `user_id`
  - Live events by `stream_id`
- Delivery semantics: at-least-once with idempotent consumers
- DLQ required for each consumer group

### 16.8 Live and Replay Policy
- Live latency target: standard HLS for MVP stability
- Replay visibility default: public, creator override supported
- Replay processing policy: 3 retries with backoff before manual intervention

### 16.9 Go-Live Gates (Pass/Fail)
- Load test passes expected peak plus 30% headroom
- Security validation passes for token rotation and signed playback URLs
- Search freshness SLO holds under synthetic publish bursts
- Runbooks validated for:
  - Kafka lag spike
  - OpenSearch indexing failure
  - Livestream start failures
- Backup restore drill executed successfully

---

## 17) ADR Registry

Architecture decisions are recorded in `docs/adr/README.md` and tracked as individual ADR files.

- `docs/adr/0001-service-architecture-modular-services.md`
- `docs/adr/0002-api-style-rest-first.md`
- `docs/adr/0003-language-split-java-go.md`
- `docs/adr/0004-event-backbone-kafka.md`
- `docs/adr/0005-opensearch-derived-index.md`
- `docs/adr/0006-jwt-refresh-rotation.md`
- `docs/adr/0007-postgres-source-of-truth.md`
- `docs/adr/0008-comments-filter-and-manual-moderation.md`
- `docs/adr/0009-livestream-replay-default-public.md`
- `docs/adr/0010-discovery-balanced-ranking.md`
- `docs/adr/0011-4k-access-for-all-users.md`
- `docs/adr/0012-chat-retention-seven-days.md`

---

## 18) Contract Documents

Implementation starts from these contract docs:

- REST API contract: `docs/contracts/rest-api-v1.md`
- Kafka event contract: `docs/contracts/kafka-event-catalog.md`

---

## 19) Implementation Status Snapshot

Current completed slices:

- CI baseline + lint gates are in place (`docs-and-contracts`, `java-quality`, `java-tests-identity`)
- Java multi-module service skeleton is complete
- Identity API stubs and response/error contracts are implemented
- Identity test baseline is implemented (controller contract tests, exception handler tests, DTO validation tests)
- Identity persistence foundation is implemented:
  - Flyway migrations for `users`, `user_credentials`, `oauth_accounts`, `sessions`, `refresh_tokens`
  - JPA entities and Spring Data repositories
  - DataJpa repository tests for uniqueness constraints and lookup queries
- Identity social login testing mode uses a fake Google token verifier with format `fake-google:<subject>:<email>`
- Identity token/session core is implemented:
  - JWT access token issuing
  - refresh token rotation and reuse detection
  - max-5 active session cap enforcement
  - `/v1/auth/refresh` is fully wired
- Identity login/social-login/logout flows are implemented end-to-end
- Content-service persistence foundation is in progress:
  - channel, channel_members, and content schema migrations
  - JPA entities and repositories for channels/members/content
  - DataJpa repository tests for uniqueness and query behavior
- Content-service channel APIs are implemented:
  - explicit channel create endpoint with owner membership assignment
  - channel lookup by id and slug
  - user channel listing endpoint
- Content-service draft/update metadata APIs are implemented:
  - `POST /v1/content` creates content in `DRAFT`
  - `PATCH /v1/content/{content_id}` updates metadata fields
  - channel membership checks enforced for create/update

Current completed slices:

- Discovery-service search read API:
  - `GET /v1/search` runs on top of the derived content index
  - the first search slice uses page/size pagination
- Discovery-service autocomplete API:
  - `GET /v1/search/autocomplete` serves title suggestions from indexed content
  - autocomplete keeps a small size limit and still defers advanced filters
- Policy-service evaluation API:
  - `POST /v1/policy/content/{content_id}/evaluate` returns reusable policy decisions
  - evaluates moderation, age restriction, and geo policy in one decision flow

Next active slice:

- Policy enforcement integration:
  - wire policy evaluation into playback, search, and discovery home filtering
  - keep downstream enforcement wiring as follow-up PRs per service
