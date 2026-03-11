#!/usr/bin/env bash
# Start CloudMedia local development infrastructure.
# Usage: ./scripts/dev/start.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT"

# Copy .env.example if .env does not exist
if [[ ! -f .env ]]; then
  echo "No .env file found — copying from .env.example"
  cp .env.example .env
fi

echo "Starting infrastructure containers..."
docker compose up -d

echo ""
echo "Waiting for services to become healthy..."

services=(cloudmedia-postgres cloudmedia-kafka cloudmedia-redis)
for svc in "${services[@]}"; do
  printf "  %-25s" "$svc"
  timeout=60
  elapsed=0
  while [[ "$(docker inspect --format='{{.State.Health.Status}}' "$svc" 2>/dev/null)" != "healthy" ]]; do
    if (( elapsed >= timeout )); then
      echo "TIMEOUT"
      echo "ERROR: $svc did not become healthy within ${timeout}s"
      exit 1
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  echo "healthy ✓"
done

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║          CloudMedia Local Dev — Ready                      ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  Postgres   localhost:${POSTGRES_PORT:-5432}                            ║"
echo "║  Kafka      localhost:${KAFKA_PORT:-9092}                            ║"
echo "║  Redis      localhost:${REDIS_PORT:-6379}                            ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  Run a service:                                            ║"
echo "║  cd services/java                                          ║"
echo "║  SPRING_PROFILES_ACTIVE=local mvn -pl identity-service \   ║"
echo "║      -am spring-boot:run                                   ║"
echo "╚══════════════════════════════════════════════════════════════╝"
