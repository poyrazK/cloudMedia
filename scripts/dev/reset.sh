#!/usr/bin/env bash
# Reset CloudMedia local development infrastructure (destroys ALL data).
# Usage: ./scripts/dev/reset.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT"

echo "WARNING: This will destroy all local development data."
read -rp "Continue? [y/N] " confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
  echo "Aborted."
  exit 0
fi

echo "Stopping containers and removing volumes..."
docker compose down -v
echo "Done. Run scripts/dev/start.sh to start fresh."
