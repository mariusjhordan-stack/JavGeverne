#!/usr/bin/env bash
# Creates .env from .env.example with freshly generated secrets. Refuses to overwrite an existing .env.
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -f .env ]; then
  echo ".env already exists; delete it first if you really want new secrets." >&2
  exit 1
fi
secret() { openssl rand -base64 96 | tr -dc 'A-Za-z0-9' | head -c "$1"; }
cp .env.example .env
for n in 20 24 48 64; do
  while grep -q "__GENERATE_${n}__" .env; do
    value=$(secret "$n")
    awk -v pat="__GENERATE_${n}__" -v val="$value" '!done && index($0, pat) { sub(pat, val); done = 1 } { print }' .env > .env.tmp
    mv .env.tmp .env
  done
done
chmod 600 .env
echo "Created .env with random secrets. Administrator: $(grep '^ADMIN_EMAIL=' .env | cut -d= -f2) (password in .env)."
