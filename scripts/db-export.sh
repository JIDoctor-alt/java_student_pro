#!/usr/bin/env bash
# Export student_pro database snapshot for version control
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT/server/server/src/main/resources/sql/data"
OUT_FILE="$OUT_DIR/student_pro_snapshot.sql"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-student_pro}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-mysql-container}"

mkdir -p "$OUT_DIR"
TMP="$(mktemp)"

cat > "$TMP" <<EOF
-- student_pro database snapshot
-- exported at: $(date '+%Y-%m-%d %H:%M:%S %z')
-- database: ${MYSQL_DATABASE}
-- NOTE: contains hashed passwords and chat history; do not publish production data to public repos.

EOF

dump_ok=0
if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$MYSQL_CONTAINER"; then
  docker exec "$MYSQL_CONTAINER" mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" \
    --single-transaction --routines --triggers --set-gtid-purged=OFF --hex-blob \
    --default-character-set=utf8mb4 "$MYSQL_DATABASE" >> "$TMP" 2>/dev/null && dump_ok=1
  echo "[db-export] exported via docker container: $MYSQL_CONTAINER"
elif command -v mysqldump >/dev/null 2>&1; then
  mysqldump -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" \
    --single-transaction --routines --triggers --set-gtid-purged=OFF --hex-blob \
    --default-character-set=utf8mb4 "$MYSQL_DATABASE" >> "$TMP" 2>/dev/null && dump_ok=1
  echo "[db-export] exported via local mysqldump"
fi

if [ "$dump_ok" -ne 1 ]; then
  rm -f "$TMP"
  echo "[db-export] failed: container '$MYSQL_CONTAINER' not running and local mysqldump unavailable" >&2
  exit 1
fi

mv -f "$TMP" "$OUT_FILE"
echo "[db-export] saved -> $OUT_FILE ($(du -h "$OUT_FILE" | cut -f1))"
