#!/usr/bin/env bash
# Runs the six services directly on this machine against a local PostgreSQL (no Docker needed for the JVMs).
#   scripts/dev-stack.sh start [--build] [--env FILE]   scripts/dev-stack.sh stop   scripts/dev-stack.sh status
# --env FILE layers extra settings over .env (e2e/stack.env points payments at the Mercado Pago test double).
# PostgreSQL must be reachable at POSTGRES_HOST:POSTGRES_PORT (default localhost:5432), e.g. `docker compose up -d postgres`.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/.run"
SERVICES="auth-service:8081 catalog-service:8082 order-service:8083 payment-service:8084 notification-service:8085 api-gateway:8080"

load_env() {
  [ -f "$ROOT/.env" ] || { echo "No .env found. Run scripts/gen-env.sh first." >&2; exit 1; }
  set -a; . "$ROOT/.env"; set +a
  if [ -n "${EXTRA_ENV:-}" ]; then set -a; . "$EXTRA_ENV"; set +a; fi
  export POSTGRES_HOST="${POSTGRES_HOST:-localhost}" POSTGRES_PORT="${POSTGRES_PORT:-5432}"
  export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
  export AUTH_SERVICE_URL=http://localhost:8081 CATALOG_SERVICE_URL=http://localhost:8082 ORDER_SERVICE_URL=http://localhost:8083
  export PAYMENT_SERVICE_URL=http://localhost:8084 NOTIFICATION_SERVICE_URL=http://localhost:8085
}

stop() {
  for entry in $SERVICES; do
    name="${entry%%:*}"
    if [ -f "$RUN/$name.pid" ]; then
      kill "$(cat "$RUN/$name.pid")" 2>/dev/null || true
      rm -f "$RUN/$name.pid"
    fi
  done
  echo "Stopped."
}

status() {
  for entry in $SERVICES; do
    name="${entry%%:*}"; port="${entry##*:}"
    code=$(curl -s -m 3 -o /dev/null -w '%{http_code}' "http://localhost:$port/actuator/health" || true)
    printf '%-22s %s\n' "$name" "$([ "$code" = 200 ] && echo UP || echo "DOWN ($code)")"
  done
}

start() {
  BUILD=0; EXTRA_ENV=""
  while [ $# -gt 0 ]; do
    case "$1" in
      --build) BUILD=1 ;;
      --env) shift; EXTRA_ENV="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")" ;;
    esac
    shift
  done
  load_env
  mkdir -p "$RUN/logs"
  for entry in $SERVICES; do
    name="${entry%%:*}"
    jar="$ROOT/backend/$name/target/$name-1.0.0.jar"
    if [ "$BUILD" = 1 ] || [ ! -f "$jar" ]; then
      echo "Building $name..."
      (cd "$ROOT/backend/$name" && mvn -B -q -DskipTests package)
    fi
  done
  stop >/dev/null
  cd "$RUN"
  for entry in $SERVICES; do
    name="${entry%%:*}"
    nohup java -Xmx256m -jar "$ROOT/backend/$name/target/$name-1.0.0.jar" > "$RUN/logs/$name.log" 2>&1 < /dev/null &
    echo $! > "$RUN/$name.pid"
  done
  echo "Waiting for services..."
  for _ in $(seq 1 90); do
    up=0
    for entry in $SERVICES; do
      port="${entry##*:}"
      [ "$(curl -s -m 2 -o /dev/null -w '%{http_code}' "http://localhost:$port/actuator/health" || true)" = 200 ] && up=$((up + 1))
    done
    [ "$up" = 6 ] && break
    sleep 2
  done
  status
  [ "$up" = 6 ] || { echo "Some services did not start; see $RUN/logs" >&2; exit 1; }
}

case "${1:-}" in
  start) shift; start "$@" ;;
  stop) stop ;;
  status) status ;;
  *) echo "usage: $0 start [--build] [--env FILE] | stop | status" >&2; exit 2 ;;
esac
