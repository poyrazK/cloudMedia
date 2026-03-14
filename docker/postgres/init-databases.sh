#!/usr/bin/env bash
# Creates per-service databases on first Postgres startup.
# This script is mounted into /docker-entrypoint-initdb.d/ and runs
# automatically only when the data directory is empty (first run).
set -euo pipefail

databases=(
  cloudmedia_identity
  cloudmedia_content
  cloudmedia_social
  cloudmedia_policy
  cloudmedia_discovery
)

for db in "${databases[@]}"; do
  echo "Creating database: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-SQL
    SELECT 'CREATE DATABASE $db OWNER $POSTGRES_USER'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
SQL
done

echo "All service databases created successfully."
