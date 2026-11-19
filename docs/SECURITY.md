# Security

## Identity and authorisation

* **auth-service** hashes passwords with BCrypt and signs access tokens with **HS512** (15 minutes). Refresh tokens
  (7 days) are single use: each is stored, revoked when exchanged, and presenting a used one revokes the whole session.
  Logout and password changes revoke refresh tokens.
* **The gateway is the only place a token is verified.** It checks signature, `HS512`, `type=access` and role, then removes any
  `X-User-Id`, `X-User-Email`, `X-User-Role` and `X-Internal-Token` the client sent and adds its own from the verified token.
  `Authorization` is forwarded only to auth-service.
* **Services trust those headers only together with `X-Internal-Token`** (`INTERNAL_API_TOKEN`, 32+ characters). A request that reaches a
  service directly without it is anonymous. `/internal/**` (stock reservations, payment results, notifications) requires that token and
  is **never routed** by the gateway.
* Rules: public = catalogue reads, register, login, refresh, the payment webhook; authenticated = everything else under `/api`;
  `/api/admin/**` = ADMIN. Every service re-enforces its own rules. Orders, payments and addresses are owner-scoped (404 for others).
* Nobody sets an order to PAID by hand: payment states come only from payment-service after it verifies the payment with Stripe;
  administrators may only move paid orders through fulfilment.

## Other controls

* **No default secrets.** `JWT_SECRET` (64+ bytes) and `INTERNAL_API_TOKEN` have no fallback; services refuse to start without them.
  `scripts/gen-env.sh` generates them. Demo users exist only in the `dev` profile.
* **Prices, totals and stock are decided on the server.** Orders accept only product ids and quantities.
* **Webhook:** HMAC-SHA256 signature mandatory (fail closed when no secret), timestamp window, constant-time comparison, payment re-fetched from the provider.
* Rate limit on login/register/refresh at the gateway (per client address, in memory: one gateway instance).
* CORS limited to `ALLOWED_ORIGINS`, no credentials. Security headers from the gateway and nginx/Caddy (HSTS in production).
* Errors are `{error, code}` without stack traces or class names. Actuator exposes only `health` without details.
* SQL is parameterised (JPA); search input is escaped; page size is capped; sort is a whitelist.
* Stripe secret and webhook keys exist only in payment-service's environment, never in the frontend.
* Containers run as non-root; PostgreSQL is not published in production.

## Known limits

* Access tokens live in `localStorage` (XSS would expose them). A cookie-based session would be stronger but needs CSRF handling.
* The rate limiter is per-process and counts by client address; behind Caddy set `TRUSTED_PROXY_COUNT` correctly.
* A stolen access token is valid until it expires (15 minutes); there is no revocation list for access tokens.
* Internal traffic is plain HTTP on the private Docker network, authenticated by a shared token (no mTLS).
* No account lockout, e-mail verification or password reset.
