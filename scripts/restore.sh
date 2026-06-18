#!/usr/bin/env bash
# Restore a gzipped pg_dump file into the running Postgres container.
# Usage:  ./scripts/restore.sh /var/backups/garage/garage-2026-06-19_0200.sql.gz
#
# WARNING: this overwrites the current database. Run against a throwaway
# container periodically to verify your backups actually restore.

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <path-to-dump.sql.gz>" >&2
  exit 1
fi

DUMP_FILE="$1"
CONTAINER="${CONTAINER:-garage-postgres}"

if [[ -f "${ENV_FILE:-}" ]]; then
  # shellcheck disable=SC1090
  set -a; source "$ENV_FILE"; set +a
fi

DB_NAME="${POSTGRES_DB:-garage}"
DB_USER="${POSTGRES_USER:-garage}"

if [[ ! -s "$DUMP_FILE" ]]; then
  echo "ERROR: $DUMP_FILE does not exist or is empty" >&2
  exit 1
fi

echo "About to restore $DUMP_FILE into $DB_NAME on $CONTAINER."
read -r -p "Type 'yes' to continue: " confirm
[[ "$confirm" == "yes" ]] || { echo "aborted"; exit 1; }

gunzip -c "$DUMP_FILE" \
  | docker exec -i \
      -e PGPASSWORD="${POSTGRES_PASSWORD:?POSTGRES_PASSWORD must be set}" \
      "$CONTAINER" \
      psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1

echo "Restore complete."
