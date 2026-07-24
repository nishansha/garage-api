#!/usr/bin/env bash
# Dump the Postgres DB from the running container to a gzipped SQL file.
# Intended to be run on the host via cron, e.g.:
#   0 2 * * *  /opt/garage-api/scripts/backup.sh >> /var/log/garage-backup.log 2>&1

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/garage}"
CONTAINER="${CONTAINER:-garage-postgres}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

# Load DB creds. Either source the project .env or pass them in the environment.
if [[ -f "${ENV_FILE:-}" ]]; then
  # shellcheck disable=SC1090
  set -a; source "$ENV_FILE"; set +a
fi

DB_NAME="${POSTGRES_DB:-garage}"
DB_USER="${POSTGRES_USER:-garage}"

# Check this before anything else touches disk — a `${VAR:?msg}` failure deep inside
# the dump pipeline below still lets gzip write an empty-but-"valid" .gz file first
# (bash forks pipeline stages before the failing expansion aborts the script), which
# then slips past the integrity check further down. Failing here creates zero files.
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD must be set}"

mkdir -p "$BACKUP_DIR"
TIMESTAMP="$(date +%F_%H%M)"
OUTFILE="$BACKUP_DIR/garage-$TIMESTAMP.sql.gz"

echo "[$(date -Iseconds)] dumping $DB_NAME from $CONTAINER -> $OUTFILE"

# Run with errexit off so a failing dump doesn't kill the script before the cleanup
# below runs — we need PIPESTATUS[0] (pg_dump's own exit code, not gzip's) to know
# whether the dump itself succeeded, independent of gzip happily compressing nothing.
set +e
docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" \
  "$CONTAINER" \
  pg_dump -U "$DB_USER" -d "$DB_NAME" --no-owner --clean --if-exists \
  | gzip -9 > "$OUTFILE"
DUMP_STATUS=${PIPESTATUS[0]}
set -e

if [[ "$DUMP_STATUS" -ne 0 ]]; then
  echo "[$(date -Iseconds)] ERROR: pg_dump failed (exit $DUMP_STATUS): $OUTFILE" >&2
  rm -f "$OUTFILE"
  exit 1
fi

# Sanity check: gzip file is non-empty and decompresses cleanly.
if [[ ! -s "$OUTFILE" ]] || ! gzip -t "$OUTFILE" 2>/dev/null; then
  echo "[$(date -Iseconds)] ERROR: backup file is empty or corrupt: $OUTFILE" >&2
  rm -f "$OUTFILE"
  exit 1
fi

SIZE="$(du -h "$OUTFILE" | cut -f1)"
echo "[$(date -Iseconds)] ok, size=$SIZE"

# Rotate: delete dumps older than RETENTION_DAYS.
find "$BACKUP_DIR" -name 'garage-*.sql.gz' -type f -mtime "+$RETENTION_DAYS" -print -delete

# Optional off-box sync. Uncomment and configure once you've set up rclone/aws/etc.
# rclone copy "$OUTFILE" remote:garage-backups/
