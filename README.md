# CloudMedia Backend

CloudMedia is a backend-first, service-oriented platform for user-generated media (YouTube-style MVP).

Current implementation is centered on Java Spring Boot domain services, with infrastructure and API contracts already in place for broader multi-language expansion.

## What is in this repo

- Java microservices for identity, content, social, policy, and discovery
- Local development infrastructure via Docker Compose (Postgres, Kafka, Redis)
- REST API contract and backend implementation planning docs
- CI with Markdown linting, Java quality gates, and identity-service test coverage checks

## Services (current)

Java services under `services/java`:

- `identity-service` (`:8081`) - auth/session/token flows
- `content-service` (`:8082`) - content metadata and playback gating
- `social-service` (`:8083`) - follows/comments/playlists
- `policy-service` (`:8084`) - moderation + age/geo policy evaluation
- `discovery-service` (`:8085`) - search and home feed filtering/ranking

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker + Docker Compose

## Quick start

1. Start local infrastructure:

```bash
make dev-up
```

This starts:

- Postgres on `localhost:5432`
- Kafka on `localhost:9092`
- Redis on `localhost:6379`

2. Run a service (example: identity-service):

```bash
cd services/java
SPRING_PROFILES_ACTIVE=local mvn -pl identity-service -am spring-boot:run
```

3. Stop or reset infrastructure when needed:

```bash
make dev-down   # keep data
make dev-reset  # destroy data
```

## Local development commands

From `services/java`:

```bash
# Run formatter + style checks for one module
mvn -pl discovery-service spotless:apply checkstyle:check

# Run tests for one module
mvn -pl discovery-service test

# Validate all Java modules quickly
mvn -f pom.xml -DskipTests validate
```

## API and planning docs

- REST API contract: `docs/contracts/rest-api-v1.md`
- MVP backend plan: `docs/mvp-backend-implementation-plan.md`
- Modular rollout roadmap: `docs/modular-implementation-roadmap.md`

## Repository layout

- `services/java` - Java services monorepo parent + modules
- `scripts/dev` - local infra lifecycle scripts
- `docker` + `docker-compose.yml` - local infra definitions
- `docs` - contracts, architecture notes, and implementation plans
- `.github/workflows/ci.yml` - CI pipeline

## Notes

- `.env` is auto-created from `.env.example` if missing when you run `make dev-up`.
- Discovery's OpenSearch integration is present but disabled by default in local config.
- Go-based services are part of the target architecture but are not scaffolded in this repo yet.
