# Deployment

Nothing here has been deployed yet; these are the reproducible steps and the decisions behind them.

## Recommended: one VPS + Docker Compose + Caddy

A 2 vCPU / 4 GB VPS (about 6–12 USD/month) runs everything: six JVMs (≈ 256–512 MB each), PostgreSQL and Caddy. Services never sleep, the webhook is always reachable, and
the architecture is unchanged. Caddy obtains and renews the HTTPS certificate by itself.

```
Internet ─▶ Caddy :80/:443 ─▶ storefront (nginx) ─▶ /api ─▶ gateway ─▶ services ─▶ PostgreSQL
Stripe ─▶ https://cronosgeneve.com/api/payments/webhook (same path)
```

1. Buy the domain; create DNS `A` records for `cronosgeneve.com` and `www` pointing at the server. Same-origin hosting means no CORS and no `api.` subdomain is needed
   (use `api.cronosgeneve.com` only if the storefront moves to separate static hosting; then set `ALLOWED_ORIGINS` and `VITE_API_URL`).
2. On the server (Ubuntu): install Docker, clone the repository, open ports 22/80/443 only.
3. `scripts/gen-env.sh`, then edit `.env`: `DOMAIN`, `FRONTEND_URL=https://cronosgeneve.com`, `ALLOWED_ORIGINS=https://cronosgeneve.com`, `ADMIN_EMAIL`/`ADMIN_PASSWORD`,
   `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, optional SMTP, and `chmod 600 .env`.
4. `docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env up -d --build`. Only Caddy is published; PostgreSQL and the gateway are private.
5. Check `https://cronosgeneve.com`, sign in with the admin from `.env`, and run the Stripe test-mode flow ([PAYMENTS.md](PAYMENTS.md)) from the real domain **before** switching to production credentials.
6. Backups: the `backup` service writes a daily dump of each database to `./backups` (14 days). Copy them off the server regularly; test a restore once.

`TRUSTED_PROXY_COUNT` is 2 in the production overlay (Caddy → nginx → gateway). The `prod` profile creates no demo users.

## Alternative: Render or Railway

Deploy the gateway and the storefront as public web services and the five other services as **private** services; use the provider's managed PostgreSQL with five databases
(or five small instances) and set each `POSTGRES_HOST/PORT`, `*_SERVICE_URL`, `JWT_SECRET`, `INTERNAL_API_TOKEN`. Costs more (six always-on services) and free tiers sleep, which breaks webhooks and
slows the first request after idle; use paid instances. The provider supplies HTTPS and the domain mapping. The Dockerfiles work unchanged.

## Before real customers

* Remove the sample catalogue (migrations V4/V7) and load your own products and photographs ([IMAGE-SOURCES.md](IMAGE-SOURCES.md)).
* Confirm the Stripe account and product currencies are configured as intended.
* Replace the academic notices with real legal pages (terms, privacy, returns).
* Consider managed PostgreSQL with point-in-time recovery, log shipping, uptime monitoring and moving tokens to HttpOnly cookies.
