# CloudMedia Modular Implementation Roadmap

This roadmap breaks implementation into small, reviewable slices with one primary service focus per PR.

## Delivery workflow

1. Pick one service scope only.
2. Open a PR with a small surface area.
3. Require CI pass before merge.
4. Merge and tag milestone.

## PR path (recommended order)

### PR-001: CI and contract baseline
- Add GitHub Actions pipeline for docs/contracts checks.
- Add Java/Go readiness checks that skip if directories are missing.
- Keep this PR infra-only.

### PR-002: Java multi-module skeleton
- Add root Java aggregator and five Spring Boot service modules.
- Each module includes app entrypoint and health endpoint stub.

### PR-003: identity-service MVP
- Phase A (done): auth controller stubs and request/response contracts.
- Phase B (next): JWT + refresh rotation plumbing and storage interfaces.
- Phase C (next): login/social/refresh/logout service logic + tests.

### PR-004: content-service MVP
- Draft, publish, unpublish endpoints.
- Playback metadata endpoint with policy integration contract.

### PR-005: policy-service MVP
- Age restriction and geo policy CRUD.
- Moderation visibility state endpoint.

### PR-006: social-service MVP
- Comments create/edit/report.
- Follows and playlists CRUD basics.

### PR-007: discovery-service MVP
- Balanced feed endpoint (followed + trending + fresh + similar).

### PR-008: OpenSearch integration
- Kafka consumers for index upsert/delete events.
- `/v1/search` and `/v1/search/autocomplete` endpoints.

### PR-009: livestream and chat foundation
- Stream lifecycle endpoints.
- Basic room messaging and anti-spam limits.

### PR-010: replay automation and hardening
- Auto replay-to-VOD trigger and status endpoint.
- DLQ, retry policies, and observability metrics.

## Branch strategy

- Base branch: `main`
- Feature branch naming:
  - `feat/identity-auth-mvp`
  - `feat/content-publish-mvp`
  - `feat/social-comments-mvp`
- Keep branches short-lived and single-purpose.

## Merge policy

- At least one review approval.
- CI green required.
- No direct pushes to `main` after bootstrap.

## Current progress

- PR-001: completed
- PR-002: completed
- PR-003: in progress (Phase A completed)
