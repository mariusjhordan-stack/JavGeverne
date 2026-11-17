#!/bin/sh
# Daily compressed dump of every service database; keeps BACKUP_KEEP_DAYS days. Restore with:
#   gunzip -c backups/<db>-<date>.sql.gz | psql -h postgres -U $POSTGRES_USER <db>
set -eu
while true; do
  stamp=$(date +%Y%m%d-%H%M%S)
  for db in auth_db catalog_db order_db payment_db notification_db; do
    pg_dump --no-owner "$db" | gzip > "/backups/$db-$stamp.sql.gz"
  done
  find /backups -name '*.sql.gz' -mtime +"${BACKUP_KEEP_DAYS:-14}" -delete
  echo "$(date) backup complete"
  sleep 86400
done
