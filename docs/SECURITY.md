# Security

## Authentication: JWT

Cronos Geneve uses stateless JWT authentication with Spring Security.

### Token Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant G as Gateway
    participant A as Auth Service

    C->>G: POST /api/auth/login {email, password}
    G->>A: Forward
    A->>A: Validate credentials (BCrypt)
    A->>A: Generate JWT (HS256)
    A-->>C: {token, refreshToken, email, role}

    Note over C: Store token in localStorage

    C->>G: GET /api/orders<br/>Authorization: Bearer {token}
    G->>G: Forward with headers
    Note over G: Gateway passes token through;<br/>each service validates independently
```

### JWT Structure

**Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload:**
```json
{
  "sub": "user@example.com",
  "role": "USER",
  "userId": "uuid",
  "iat": 1700000000,
  "exp": 1700003600
}
```

### Token Configuration

| Parameter | Default | Description |
|-----------|---------|-------------|
| Algorithm | HS256 | HMAC-SHA256 |
| Secret | env `JWT_SECRET` | Min 256 bits |
| Access TTL | 1 hour | `JWT_EXPIRATION` |
| Refresh TTL | 24 hours | `JWT_REFRESH_EXPIRATION` |

## Password Security

- **Hashing:** BCrypt with default strength (10 rounds)
- **Storage:** Only the hash is stored; plaintext passwords are never logged or persisted
- **Validation:** Minimum 6 characters enforced via `@Size` annotation

## Role-Based Access Control

| Role | Capabilities |
|------|-------------|
| USER | Browse catalog, manage cart, place orders, view own orders/profile |
| ADMIN | Everything USER can do, plus create/update products, manage stock |

### Endpoint Protection

| Endpoint Pattern | Access |
|-----------------|--------|
| `/api/auth/register`, `/api/auth/login` | Public |
| `/api/products` (GET) | Public |
| `/api/products` (POST/PUT) | ADMIN only |
| `/api/orders/**` | Authenticated |
| `/api/payments/webhook` | Public (HMAC validated) |
| `/actuator/health` | Public |

## CORS Configuration

CORS is configured at the API Gateway level:

- **Allowed Origins:** Configurable via `ALLOWED_ORIGINS` env var
- **Allowed Methods:** GET, POST, PUT, PATCH, DELETE, OPTIONS
- **Allowed Headers:** Authorization, Content-Type, X-User-Id, X-User-Email
- **Credentials:** Enabled
- **Max Age:** 3600 seconds

## Webhook HMAC Validation

Mercado Pago webhooks are validated using HMAC-SHA256:

1. Extract `x-signature` header (format: `ts={timestamp},v1={hash}`)
2. Extract `x-request-id` header
3. Construct message: `id:{data.id};request-id:{x-request-id};ts:{ts};`
4. Compute HMAC-SHA256 with the webhook secret
5. Compare computed hash with `v1` value
6. **Reject** if signature is missing, malformed, or mismatched

No bypass mechanisms exist. Test webhooks must include valid signatures.

## Input Validation

- All DTOs use Jakarta Validation annotations (`@NotBlank`, `@Email`, `@Size`, `@NotNull`, `@Positive`)
- Controllers annotate request bodies with `@Valid`
- Global exception handler returns structured error responses for validation failures

## Security Best Practices

1. **No plaintext secrets** - All sensitive values come from environment variables
2. **No security bypass** - Security is never disabled, even for tests (tests use proper test config)
3. **No credential logging** - Passwords and tokens are never written to logs
4. **Stateless sessions** - `SessionCreationPolicy.STATELESS` prevents session fixation
5. **CSRF disabled** - Appropriate for stateless JWT APIs
6. **Password hashing** - BCrypt is used exclusively; no MD5, SHA, or plaintext
7. **Token expiration** - Both access and refresh tokens expire
8. **Idempotency keys** - Payment webhook processing is idempotent
9. **Amount validation** - Payment amounts are verified against the original order
10. **HTTPS ready** - Production deployment expects HTTPS termination at the load balancer
