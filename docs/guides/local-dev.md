# Local Development Guide

Run the backend local infrastructure with Docker Compose. Spring local profiles currently wire Postgres, while Kafka and Redis are started and ready for service integrations.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) (with Compose v2)
- Java 21 (Temurin recommended)
- Maven 3.9+

## Quick Start

```bash
# 1. Start infrastructure (Postgres, Kafka, Redis)
make dev-up

# 2. Run a service against local infra
cd services/java
SPRING_PROFILES_ACTIVE=local mvn -pl identity-service -am spring-boot:run
```

The service will boot, Flyway will run migrations, and the API will be available.

## Connection Details

| Service    | Host        | Port   | Notes                          |
|------------|-------------|--------|--------------------------------|
| Postgres   | `localhost` | `5432` | User: `cloudmedia`, Pass: `localdev` |
| Kafka      | `localhost` | `9092` | KRaft mode (no Zookeeper)      |
| Redis      | `localhost` | `6379` | No password                    |

### Per-Service Databases

| Service           | Database              | App Port |
|-------------------|-----------------------|----------|
| identity-service  | `cloudmedia_identity` | 8081     |
| content-service   | `cloudmedia_content`  | 8082     |
| social-service    | `cloudmedia_social`   | 8083     |
| policy-service    | `cloudmedia_policy`   | 8084     |
| discovery-service | `cloudmedia_discovery`| 8085     |

## Running Each Service

All services use the `local` Spring profile to connect to local Postgres in Docker (Kafka and Redis containers are also available):

```bash
cd services/java

# identity-service (port 8081)
SPRING_PROFILES_ACTIVE=local mvn -pl identity-service -am spring-boot:run

# content-service (port 8082)
SPRING_PROFILES_ACTIVE=local mvn -pl content-service -am spring-boot:run

# social-service (port 8083)
SPRING_PROFILES_ACTIVE=local mvn -pl social-service -am spring-boot:run

# policy-service (port 8084)
SPRING_PROFILES_ACTIVE=local mvn -pl policy-service -am spring-boot:run

# discovery-service (port 8085)
SPRING_PROFILES_ACTIVE=local mvn -pl discovery-service -am spring-boot:run
```

Verify a service is running:

```bash
curl http://localhost:8081/actuator/health
# → {"status":"UP"}
```

## Developer Scripts

| Script                    | Action                                          |
|---------------------------|-------------------------------------------------|
| `scripts/dev/start.sh`   | Start infra, wait for health, print connection info |
| `scripts/dev/stop.sh`    | Stop containers (data is preserved)             |
| `scripts/dev/reset.sh`   | Stop containers AND delete all data (clean slate) |

`make dev-up`, `make dev-down`, and `make dev-reset` are convenience wrappers around these scripts.

## Customizing Ports

Copy `.env.example` to `.env` and edit to change default ports:

```bash
cp .env.example .env
# Edit .env to change POSTGRES_PORT, KAFKA_PORT, REDIS_PORT
```

## Connecting with CLI Tools

```bash
# Postgres (psql)
docker compose exec postgres psql -U cloudmedia -d cloudmedia_identity

# Redis (redis-cli)
docker compose exec redis redis-cli

# Kafka (list topics)
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

## Troubleshooting

### Port already in use

Change the conflicting port in `.env` and restart:

```bash
# Example: Postgres on 5433 instead of 5432
echo "POSTGRES_PORT=5433" >> .env
make dev-down
make dev-up
```

Update the corresponding `application-local.yml` or set the env var:

```bash
POSTGRES_PORT=5433 SPRING_PROFILES_ACTIVE=local mvn -pl identity-service -am spring-boot:run
```

### Flyway migration errors after reset

After `scripts/dev/reset.sh`, databases are recreated empty. Flyway will re-run all migrations on next service start. If you see migration checksum errors, reset the data:

```bash
make dev-reset
make dev-up
```

### Container keeps restarting

Check logs for the failing container:

```bash
docker compose logs postgres
docker compose logs kafka
docker compose logs redis
```
