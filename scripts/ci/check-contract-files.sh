#!/usr/bin/env bash
set -euo pipefail

required_files=(
  "docs/mvp-backend-implementation-plan.md"
  "docs/contracts/rest-api-v1.md"
  "docs/contracts/kafka-event-catalog.md"
  "docs/adr/README.md"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required file: $file"
    exit 1
  fi
done

echo "Contract and architecture docs are present."
