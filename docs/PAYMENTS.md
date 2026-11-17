# Payments (Mercado Pago Checkout Pro)

## Flow

1. The customer places an order (`POST /api/orders`): the catalogue reserves stock and returns official prices; the order is `PENDING`.
2. `POST /api/payments/preference {orderId}`: payment-service reads **amount, currency and items from the order service**, checks the caller owns it,
   creates a preference with `external_reference = orderId`, `notification_url`, return URLs and an expiry equal to the stock reservation, and returns the checkout URL.
3. The browser goes to Mercado Pago, pays, and returns to `/checkout/{success|failure|pending}?order=…`. **That page believes nothing in the URL**: it calls
   `POST /api/payments/order/{id}/sync` (the server asks Mercado Pago) and shows the order's real status.
4. Mercado Pago calls `POST /api/payments/webhook?data.id=…&type=payment` with `x-signature` and `x-request-id`.
   payment-service verifies the HMAC, **fetches the payment from Mercado Pago**, and checks reference, amount and currency against the stored checkout.
5. Each distinct provider state is recorded once (`payment_events`, unique on payment id + status) and reported to order-service, which moves the order:
   approved → `PAID` (stock confirmed, customer notified); rejected/cancelled → `PAYMENT_FAILED` (stock stays reserved for a retry); in process → `PAYMENT_PENDING`;
   refunded → `REFUNDED` (stock released). If order-service does not acknowledge, nothing is stored and the provider's retry (or the 5-minute reconciliation job) repeats it.
6. Unpaid orders are cancelled after `ORDER_RESERVATION_TTL_MINUTES` and their stock released.

## Configuration (payment-service environment)

| Variable | Meaning |
|---|---|
| `MERCADOPAGO_ACCESS_TOKEN` | Server-side access token. `TEST-…` selects the sandbox checkout URL. Empty = checkout answers 503. |
| `MERCADOPAGO_WEBHOOK_SECRET` | "Secret signature" from the Mercado Pago webhooks page. Empty = every webhook is refused. |
| `FRONTEND_URL` | Public storefront URL; return URLs default to `FRONTEND_URL/checkout/{success,failure,pending}`. |
| `MP_SUCCESS_URL`, `MP_FAILURE_URL`, `MP_PENDING_URL` | Optional overrides. |
| `MP_WEBHOOK_URL` | Public HTTPS URL of the webhook, e.g. `https://cronosgeneve.com/api/payments/webhook`. |
| `MP_WEBHOOK_MAX_AGE_SECONDS` | Accepted age of a signature timestamp (default 900). |

`auto_return` is sent only when the success URL is HTTPS (Mercado Pago rejects it otherwise).

## What is and is not proven

| Piece | Status |
|---|---|
| Our webhook/HMAC/idempotency/amount logic, state changes, stock, notifications | **Tested** against a stand-in for the API (unit, integration and e2e) |
| Preference payload and API paths match Mercado Pago's documentation | Reviewed against the documented shape; **not yet accepted by the real API** |
| Currency: all prices are `USD` | **Unverified.** Mercado Pago accounts accept only their country's currency; if your test account is, e.g., PEN, set the catalogue currency accordingly |
| Redirect, payment with a test card, real signed webhook, return page | **Not tested with Mercado Pago.** Do the Sandbox test below |

## Sandbox test (needs your accounts)

1. In the Mercado Pago developer panel create an application, then two **test users** (seller and buyer).
2. Copy the seller's **test access token** and, under *Webhooks*, set the URL to your public HTTPS endpoint (use a tunnel such as `cloudflared tunnel --url http://localhost:8080` while developing) and copy the **secret signature**.
3. Put them in `.env`: `MERCADOPAGO_ACCESS_TOKEN=TEST-…`, `MERCADOPAGO_WEBHOOK_SECRET=…`, `MP_WEBHOOK_URL=https://<tunnel>/api/payments/webhook`, `FRONTEND_URL=https://<tunnel>` (or `http://localhost:5173` for the return trip), then `docker compose up --build`.
4. Sign in, add a watch to the bag, check out, pay with a Mercado Pago test card as the buyer test user.
5. Verify: you land on `/checkout/success?order=…` and see "Payment confirmed"; `/admin/payments` shows an `approved` payment with a provider reference; the order is `PAID`; stock dropped by one; a notification exists. Repeat with a card that is rejected (expect `PAYMENT_FAILED` and a retry button).
6. Only after this passes may the integration be called working.
