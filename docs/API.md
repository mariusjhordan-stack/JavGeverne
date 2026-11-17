# API (through the gateway, `http://host:8080`)

Errors are `{ "error": "...", "code": "..." }`; validation errors also carry `fields`. Auth: `Authorization: Bearer <access token>`.

## Public
| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/register` · `/login` · `/refresh` · `/logout` | Tokens: `{token, refreshToken, email, role, firstName, lastName}`. Refresh tokens are single use |
| GET | `/api/products` | `q, brandId, categoryId, minPrice, maxPrice, movementType, featured, sort=newest\|price_asc\|price_desc\|name, page, size≤48` |
| GET | `/api/products/{id}` · `/api/products/featured` · `/api/brands` · `/api/brands/{idOrSlug}` · `/api/categories` | |
| POST | `/api/payments/webhook` | Called by Mercado Pago; authenticated by HMAC signature |

## Signed in
| Method | Path | Notes |
|---|---|---|
| GET/PUT | `/api/auth/profile` · PUT `/api/auth/password` | |
| GET/POST/PUT/DELETE | `/api/auth/addresses[/{id}]` | Owner only, max 10 |
| GET/PUT/DELETE | `/api/favorites[/{productId}]` | |
| POST | `/api/orders` `{items:[{productId, quantity}], shippingAddress}` | Prices come from the server. 409 `INSUFFICIENT_STOCK` |
| GET | `/api/orders` · `/api/orders/{id}` | Own orders, with `history` (tracking) |
| POST | `/api/orders/{id}/cancel` | Only `PENDING` / `PAYMENT_FAILED`; releases stock |
| POST | `/api/payments/preference` `{orderId}` | Returns `{checkoutUrl}`; 503 `PAYMENT_PROVIDER_NOT_CONFIGURED` without credentials |
| GET | `/api/payments/order/{orderId}` · POST `/api/payments/order/{orderId}/sync` | Sync asks Mercado Pago |
| GET | `/api/notifications` | Own notifications |

## Administrator (`/api/admin/**`)
`products` (list, get, create, update incl. `active`, `POST {id}/stock-adjustments`), `inventory/summary`, `inventory/movements`,
`orders` (list, `{id}`, `stats`, `PUT {id}/status` for `PROCESSING|SHIPPED|DELIVERED|CANCELLED`), `payments` (+ `stats`), `users` (+ `{id}`), `notifications`.

`/internal/**` exists on catalog, order and notification but is never routed by the gateway.
