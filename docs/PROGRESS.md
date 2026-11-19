# Status

Last full verification: all backend suites and the Playwright suite pass against PostgreSQL (see README for counts).
**Not verified:** anything involving the real Stripe API, the Docker images (no Docker daemon was available while developing; the compose files validate with `docker compose config`), a real SMTP server, and any internet deployment.

## Done and tested
* Gateway JWT verification, trusted identity headers, admin routes, internal paths closed, rate limiting.
* Orders priced by the server; atomic, idempotent, concurrency-tested stock reservations; order state machine with history and expiry.
* Payment flow with signed, idempotent, verified webhooks against a stand-in for the provider; sync and reconciliation.
* Notification service starts with and without SMTP and is called by order-service; status is truthful.
* Storefront: search/sort, compare, favourites, checkout, verified payment result pages, order tracking, profile/addresses/password, silent session refresh, 404.
* Administration: overview, products (create/edit/hide/stock), inventory and movements, orders (fulfilment), payments, customers.
* Editorial redesign with licensed photography ([IMAGE-SOURCES.md](IMAGE-SOURCES.md)), reduced-motion support.
* Production overlay (Caddy HTTPS, private network, non-root containers, memory limits, backups).

## Pending
* **Stripe test-mode checkout and webhook round-trip** (blocker for calling payments finished).
* **Product reviews and ratings** were not implemented (planned rules: buyers with a paid order only, one per product, edit/delete own, admin moderation).
* Frontend unit tests; accessibility audit with a screen reader; Lighthouse performance pass.
* Replace representative photographs with the shop's own; real legal pages.
* Admin: product image upload (paths are typed today), refunds from the panel, customer disabling.
