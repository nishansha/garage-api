# garage-api

## Backups

`scripts/backup.sh` dumps the Postgres DB (from the running `garage-postgres`
container) to a gzipped file in `BACKUP_DIR` (default `/var/backups/garage`),
verifies the dump isn't empty/corrupt, and rotates anything older than
`RETENTION_DAYS` (default 14). `scripts/restore.sh` restores a dump back in,
with a confirmation prompt since it overwrites the target database.

Neither script runs on its own — something has to schedule `backup.sh`.

### Scheduling (do this once per VM)

```bash
./scripts/install-backup-cron.sh
```

Idempotent — installs a daily `0 2 * * *` cron entry that runs `backup.sh`
against the repo's `.env`, safe to re-run (replaces its own previous entry,
leaves any other crontab lines alone). Verify with `crontab -l`; first run's
output lands in `/var/log/garage-backup.log`.

**Known gap, accepted for now:** backups are local-only, on the same VM/disk
as the live database. A disk failure or lost VM takes both out together. Fine
for now given the scale of this deployment — revisit (rclone to S3-compatible
storage, or rely on the VM provider's disk snapshots) if that risk profile
changes. `backup.sh` has a commented-out `rclone copy` line ready to uncomment
if/when an off-box target is set up.

### Testing a restore

A backup you've never restored is a hope, not a backup. Do this once after
setting up scheduling, and periodically after (e.g. whenever `restore.sh`
or the schema changes) — always against a **throwaway container**, never the
live `garage-postgres`:

```bash
docker run --rm -d --name garage-restore-test \
  -e POSTGRES_DB=garage -e POSTGRES_USER=garage -e POSTGRES_PASSWORD=test \
  postgres:16-alpine

# wait ~5s for it to accept connections, then restore the latest dump into it:
CONTAINER=garage-restore-test \
POSTGRES_DB=garage POSTGRES_USER=garage POSTGRES_PASSWORD=test \
  ./scripts/restore.sh /var/backups/garage/garage-<latest-timestamp>.sql.gz

# spot-check real data came back:
docker exec -e PGPASSWORD=test garage-restore-test \
  psql -U garage -d garage -c "SELECT count(*) FROM app_sale;"

docker rm -f garage-restore-test
```

**Restore drill log** — update this after each drill so "has this ever been
tested" has an actual answer instead of "probably":

| Date | Result | Notes |
|------|--------|-------|
| _(none yet)_ | | |
