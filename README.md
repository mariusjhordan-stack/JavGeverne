# Cronos Genève

Academic e-commerce demonstration for Swiss watches: six Spring Boot microservices (Java 21), a React storefront,
PostgreSQL and Mercado Pago Checkout Pro. It is **not** a real shop and is not affiliated with Rolex, Omega or TAG Heuer.

```
Browser ─▶ Storefront (nginx) ─▶ API gateway :8080 ─▶ auth :8081 · catalog :8082 · order :8083 · payment :8084 · notification :8085
                                       │ verifies the JWT, injects the user, never routes /internal
                                       └─▶ PostgreSQL (auth_db, catalog_db, order_db, payment_db, notification_db)
Mercado Pago ──webhook (HMAC)──▶ gateway ─▶ payment ─▶ order ─▶ catalog (stock) ─▶ notification
```

Read next: [architecture](docs/ARCHITECTURE.md) · [security](docs/SECURITY.md) · [payments](docs/PAYMENTS.md) ·
[API](docs/API.md) · [deployment](docs/DEPLOYMENT.md) · [images](docs/IMAGE-SOURCES.md) · [status](docs/PROGRESS.md)

## Run it locally

Requirements: Java 21, Maven 3.9, Node 22, and either Docker or a local PostgreSQL 16.

```bash
scripts/gen-env.sh          # creates .env with random secrets (never committed)
docker compose up --build   # storefront http://localhost:5173, gateway http://localhost:8080
```

Without Docker, start PostgreSQL yourself (create the five databases with `infrastructure/database/init-multiple-dbs.sh`), then:

```bash
scripts/dev-stack.sh start --build     # six services on PostgreSQL (dev profile)
cd frontend && npm ci && npm run dev   # http://localhost:5173
```

The `dev` profile also creates two demo accounts (`admin@cronos.com` / `Admin123!`, `user@cronos.com` / `User123!`) and
shows quick-fill buttons on the login page **in dev builds only**. Production uses `ADMIN_EMAIL` / `ADMIN_PASSWORD` from `.env`
instead and has no demo users. The catalogue migration seeds 12 sample watches.

Without Mercado Pago credentials the store works but checkout answers *"online payment is not available"*, and webhooks are refused.

## Tests

```bash
for s in api-gateway auth-service catalog-service order-service payment-service notification-service; do (cd backend/$s && mvn -B verify); done
scripts/e2e.sh --build      # Playwright against the real stack on PostgreSQL (needs a running PostgreSQL and .env)
```

| Suite | Tests | What it proves |
|---|---|---|
| api-gateway | 15 | JWT verification, header stripping, admin routes, `/internal` never routed, rate limit, CORS |
| auth-service | 41 | register/login/refresh rotation, profile, addresses, admin listing |
| catalog-service | 14 | search/sort, admin CRUD, atomic reservations, no overselling under concurrency, favourites |
| order-service | 10 | prices from the catalogue, ownership, state machine, payment results, expiry |
| payment-service | 13 | webhook signature, amount/currency/reference checks, idempotency, retries, fail-closed |
| notification-service | 5 | starts with and without SMTP, honest SENT/SKIPPED/FAILED, a real SMTP conversation |
| e2e (Playwright) | 25 | security and service contracts over HTTP, plus the browser purchase flow |

The e2e suite uses `e2e/mp-stub`, a stand-in for the Mercado Pago API. **It proves our handling of what the provider reports,
not that Mercado Pago accepts our requests.** The Sandbox procedure is in [docs/PAYMENTS.md](docs/PAYMENTS.md).

## Repository map

`backend/<service>` one Maven project each · `frontend` React 19 + Vite + Tailwind 4 · `e2e` Playwright and the provider stub ·
`infrastructure` database init, Caddy, backups · `scripts` env generation, local stack, e2e runner · `docs`.
