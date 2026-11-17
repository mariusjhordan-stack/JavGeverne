# Architecture

## Services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| api-gateway | 8080 | none | Verifies the JWT, injects identity, routes `/api/**`, rate limits credential endpoints, CORS, security headers |
| auth-service | 8081 | auth_db | Users, BCrypt, JWT issue, single-use refresh tokens, profile, addresses, admin user listing |
| catalog-service | 8082 | catalog_db | Products, brands, categories, favourites, inventory and atomic per-order stock reservations |
| order-service | 8083 | order_db | Orders priced from the catalogue, state machine and history, expiry of unpaid orders, fulfilment |
| payment-service | 8084 | payment_db | Mercado Pago checkout, signed webhook, verification, idempotent payment events, reconciliation |
| notification-service | 8085 | notification_db | Records and e-mails order and payment notifications |

One PostgreSQL instance, one database per service, no cross-database foreign keys. Services talk to each other over HTTP on the private network.

## Internal calls (all authenticated with `X-Internal-Token`)

| Caller → callee | Endpoint | Purpose |
|---|---|---|
| order → catalog | `POST /internal/stock/reservations` | Reserve stock for an order (all-or-nothing) and receive the official prices |
| order → catalog | `POST /internal/stock/reservations/{orderId}/release` · `/confirm` | Give stock back (cancel, expiry, refund) · mark it sold (paid). Both idempotent |
| order → notification | `POST /internal/notifications` | Order created, payment approved/rejected, cancelled, refunded, fulfilment steps. Best effort: never fails an order |
| payment → order | `GET /internal/orders/{id}` | Amount, currency, items and owner for a checkout |
| payment → order | `POST /internal/orders/{id}/payment-result` | `APPROVED | REJECTED | PENDING | REFUNDED`, applied idempotently |

There is no message broker: HTTP is enough for this scale and keeps failures visible. The trade-off is that a failed
notification is not retried (it is logged and visible in the admin API); a failed payment-result is retried by Mercado Pago and by the reconciliation job.

## Order states

`PENDING → PAYMENT_PENDING → PAID → PROCESSING → SHIPPED → DELIVERED`, with `PAYMENT_FAILED` (retry possible), `CANCELLED` and `REFUNDED`.
Payment states are set only by payment-service; administrators only move `PAID` orders through fulfilment or cancel. Every change is written to `order_status_history`, which the customer's tracking view reads.

## Stock

Stock is decremented by one conditional `UPDATE … WHERE stock >= :n`, so concurrent orders cannot oversell (tested with parallel requests).
Each reservation is stored per order (`RESERVED → CONFIRMED | RELEASED`) and every change is logged in `stock_movements`.

## Data and migrations

Flyway migrations live in each service (`db/migration`); Hibernate only validates. Tests run on H2 (PostgreSQL mode) with Flyway off; the e2e suite and
local runs use PostgreSQL. Catalogue migration V4/V7 seeds sample watches; remove them before a real launch.

## Frontend

React 19, Vite, Tailwind 4. State: auth (tokens, silent refresh), cart (local), compare (ids only). The browser never sends prices; return pages ask the
server for the verified status. Admin pages call `/api/admin/**` and are authorised again on the server.
