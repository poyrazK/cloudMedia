#!/usr/bin/env bash
# Stop CloudMedia local development infrastructure (preserves data).
# Usage: ./scripts/dev/stop.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT"

echo "Stopping infrastructure containers (data is preserved)..."
docker compose down
echo "Done. Run scripts/dev/start.sh to restart."
