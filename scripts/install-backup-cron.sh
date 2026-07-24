#!/usr/bin/env bash
# Installs (or updates) the daily DB backup cron job on this VM.
# Idempotent — safe to re-run; replaces any previous line it installed
# (tagged with MARKER below) instead of duplicating it.
#
# Usage (run once on the VM, from the repo root, as the user that has
# permission to run `docker exec` against the postgres container):
#   ./scripts/install-backup-cron.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKUP_SCRIPT="$SCRIPT_DIR/backup.sh"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"
LOG_FILE="${LOG_FILE:-/var/log/garage-backup.log}"
SCHEDULE="${SCHEDULE:-0 2 * * *}"   # daily at 02:00, matches backup.sh's own header comment
MARKER="# garage-api-backup (installed by scripts/install-backup-cron.sh)"

if [[ ! -x "$BACKUP_SCRIPT" ]]; then
  echo "ERROR: $BACKUP_SCRIPT not found or not executable" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "WARNING: $ENV_FILE not found. backup.sh needs POSTGRES_PASSWORD (and" >&2
  echo "POSTGRES_DB/POSTGRES_USER if you changed them from the default) available" >&2
  echo "via that file. Create it (or set ENV_FILE=/path/to/env before running this" >&2
  echo "installer) before the first scheduled run, or backups will silently fail." >&2
fi

CRON_LINE="$SCHEDULE ENV_FILE=$ENV_FILE $BACKUP_SCRIPT >> $LOG_FILE 2>&1 $MARKER"

# Keep everything already in the crontab; just replace our own previously
# installed line (matched by MARKER) so re-running this doesn't duplicate it.
( crontab -l 2>/dev/null | grep -vF "$MARKER"; echo "$CRON_LINE" ) | crontab -

echo "Installed cron job:"
echo "  $CRON_LINE"
echo
echo "Verify with:  crontab -l"
echo "First run's output will land in: $LOG_FILE"
