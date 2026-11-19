# Payments (Stripe Checkout)

## Flow

1. The customer places an order (`POST /api/orders`): the catalogue reserves stock and returns official prices; the order is `PENDING`.
2. `POST /api/payments/preference {orderId}`: payment-service reads the amount, currency and items from order-service, checks ownership,
   creates a Stripe Checkout Session with an expiry no later than the stock reservation, and returns Stripe's hosted checkout URL.
3. The browser goes to Stripe Checkout and returns to `/checkout/success` or `/checkout/failure`. The return page does not trust URL parameters:
   it calls `POST /api/payments/order/{id}/sync`, and payment-service retrieves the Checkout Sessions from Stripe.
4. Stripe calls `POST /api/payments/webhook`. payment-service verifies `Stripe-Signature` against the raw request body and the webhook signing
   secret, then retrieves the Checkout Session from Stripe before processing it.
5. Amount, currency and order reference are checked against the stored checkout. Distinct provider state changes are recorded once in
   `payment_events`; order-service moves the order to paid, failed, pending or refunded. If order-service does not acknowledge, the event is
   not committed and webhook retries or reconciliation can process it again.
6. Unpaid orders are cancelled after `ORDER_RESERVATION_TTL_MINUTES` and their stock released.

## Configuration

| Variable | Meaning |
|---|---|
| `STRIPE_SECRET_KEY` | Server-side Stripe secret key. Use a test-mode `sk_test_...` key while testing. Empty disables online checkout. |
| `STRIPE_WEBHOOK_SECRET` | Webhook signing secret (`whsec_...`) for the endpoint or Stripe CLI listener. Empty means every webhook is refused. |
| `FRONTEND_URL` | Storefront URL used for return URLs; defaults to `FRONTEND_URL/checkout/{success,failure}`. |
| `STRIPE_SUCCESS_URL`, `STRIPE_CANCEL_URL` | Optional return URL overrides. |
| `STRIPE_WEBHOOK_MAX_AGE_SECONDS` | Accepted signature timestamp age (default 300 seconds). |

This flow uses Stripe's hosted Checkout page, so the publishable `pk_test_...` key is not needed in the application. Never put a secret key
in frontend configuration or source control. Configure keys only in the ignored local `.env`.

## What is and is not proven

| Piece | Status |
|---|---|
| Checkout request formation, signature verification, amount/currency/reference checks, idempotency, state transitions and reconciliation | Tested against a local Stripe API stand-in |
| Checkout Session request and webhook handling against Stripe's real test-mode API | Not yet tested |
| Hosted redirect, test card, live Stripe webhook and return page | Not yet tested |

## Test-mode checkout

1. In the Stripe Dashboard, create or use a test-mode account and issue a new test secret key. If a secret key was exposed, revoke it first.
2. Create a local `.env` from `.env.example` and set `STRIPE_SECRET_KEY=sk_test_...`.
3. Start the stack and forward webhooks from the Stripe CLI:

   ```bash
   stripe listen --forward-to localhost:8080/api/payments/webhook
   ```

   Copy the CLI's `whsec_...` value into `.env` as `STRIPE_WEBHOOK_SECRET`, then restart the payment-service.
4. Sign in, add a watch to the bag, and check out. Use a Stripe test card such as `4242 4242 4242 4242` with a future expiry and any CVC.
5. Verify the return page reports a confirmed payment, the order is `PAID`, stock is confirmed, and `/admin/payments` shows the payment.
   Also test a declined test payment and confirm it does not mark the order paid.
6. Keep the keys in test mode until the full round-trip has been verified. Do not send secret keys in chat or commit them.
