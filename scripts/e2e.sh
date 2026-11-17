#!/usr/bin/env bash
# Starts everything the end-to-end suite needs and runs it:
#   services on PostgreSQL (dev profile) + Mercado Pago test double + storefront dev server.
# Requires PostgreSQL reachable at localhost:5432 and a .env (scripts/gen-env.sh).
#   scripts/e2e.sh [--build] [-- <extra playwright args>]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/.run"; mkdir -p "$RUN/logs"
BUILD=""; [ "${1:-}" = "--build" ] && { BUILD="--build"; shift; }
[ "${1:-}" = "--" ] && shift

cleanup() {
  [ -f "$RUN/mp-stub.pid" ] && kill "$(cat "$RUN/mp-stub.pid")" 2>/dev/null || true
  [ -f "$RUN/frontend.pid" ] && kill "$(cat "$RUN/frontend.pid")" 2>/dev/null || true
  rm -f "$RUN/mp-stub.pid" "$RUN/frontend.pid"
  "$ROOT/scripts/dev-stack.sh" stop >/dev/null || true
}
trap cleanup EXIT
cleanup

MERCADOPAGO_WEBHOOK_SECRET=e2e-webhook-secret nohup node "$ROOT/e2e/mp-stub/server.mjs" > "$RUN/logs/mp-stub.log" 2>&1 < /dev/null &
echo $! > "$RUN/mp-stub.pid"

"$ROOT/scripts/dev-stack.sh" start $BUILD --env "$ROOT/e2e/stack.env"

cd "$ROOT/frontend"
[ -d node_modules ] || npm ci
nohup npx vite --port 5173 --strictPort > "$RUN/logs/frontend.log" 2>&1 < /dev/null &
echo $! > "$RUN/frontend.pid"
for _ in $(seq 1 30); do curl -s -o /dev/null http://localhost:5173/ && break; sleep 1; done

cd "$ROOT/e2e"
[ -d node_modules/playwright ] || npm install
npx playwright test "$@"
