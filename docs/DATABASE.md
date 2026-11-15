# Database Design

## Strategy: Database per Service

Each microservice owns its database, enforcing strong service boundaries. No service reads or writes another service's tables directly.

| Service | Database | Description |
|---------|----------|-------------|
| Auth Service | auth_db | Users, roles, credentials |
| Catalog Service | catalog_db | Products, brands, categories |
| Order Service | order_db | Orders, order items |
| Payment Service | payment_db | Payments, preferences |
| Notification Service | notification_db | Notification history |

All databases run on a single PostgreSQL 16 instance for development. In production, they can be split into separate instances.

## Schema Migrations

Flyway manages all schema migrations. Each service has its own `db/migration/` directory with versioned SQL files following the pattern `V{version}__{description}.sql`.

Migrations run automatically on service startup.

## Schema Diagrams

### Auth Service (auth_db)

```mermaid
erDiagram
    USERS {
        UUID id PK
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR role "USER | ADMIN"
        BOOLEAN enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
```

### Catalog Service (catalog_db)

```mermaid
erDiagram
    BRANDS {
        BIGSERIAL id PK
        VARCHAR name
        TEXT description
        VARCHAR logo_url
        VARCHAR country
        TIMESTAMP created_at
    }
    CATEGORIES {
        BIGSERIAL id PK
        VARCHAR name
        TEXT description
        VARCHAR slug UK
        TIMESTAMP created_at
    }
    PRODUCTS {
        UUID id PK
        VARCHAR name
        TEXT description
        VARCHAR reference_number
        BIGINT brand_id FK
        BIGINT category_id FK
        DECIMAL price "12,2"
        VARCHAR currency
        INT stock
        VARCHAR case_material
        VARCHAR case_diameter
        VARCHAR movement_type "AUTOMATIC | MANUAL | QUARTZ"
        VARCHAR water_resistance
        VARCHAR crystal_type
        VARCHAR dial_color
        VARCHAR strap_material
        VARCHAR power_reserve
        VARCHAR image_url
        TEXT gallery_urls
        BOOLEAN featured
        BOOLEAN active
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
    BRANDS ||--o{ PRODUCTS : "has"
    CATEGORIES ||--o{ PRODUCTS : "contains"
```

### Order Service (order_db)

```mermaid
erDiagram
    ORDERS {
        UUID id PK
        UUID user_id
        VARCHAR user_email
        VARCHAR status "PENDING | PAID | CANCELLED | ..."
        DECIMAL subtotal "12,2"
        DECIMAL tax "12,2"
        DECIMAL total "12,2"
        VARCHAR currency
        TEXT shipping_address
        VARCHAR payment_id
        VARCHAR payment_status
        TEXT notes
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
    ORDER_ITEMS {
        UUID id PK
        UUID order_id FK
        UUID product_id
        VARCHAR product_name
        VARCHAR product_image
        INT quantity
        DECIMAL unit_price "12,2"
        DECIMAL total_price "12,2"
        TIMESTAMP created_at
    }
    ORDERS ||--o{ ORDER_ITEMS : "contains"
```

### Payment Service (payment_db)

```mermaid
erDiagram
    PAYMENTS {
        UUID id PK
        UUID order_id
        VARCHAR external_payment_id
        VARCHAR preference_id
        VARCHAR status "PENDING | APPROVED | REJECTED | ..."
        DECIMAL amount "12,2"
        VARCHAR currency
        VARCHAR payment_method
        VARCHAR payer_email
        VARCHAR idempotency_key UK
        TIMESTAMP webhook_received_at
        TIMESTAMP verified_at
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
```

### Notification Service (notification_db)

```mermaid
erDiagram
    NOTIFICATIONS {
        UUID id PK
        UUID user_id
        VARCHAR user_email
        VARCHAR type "ORDER_CREATED | PAYMENT_APPROVED | ..."
        VARCHAR subject
        TEXT body
        VARCHAR status "PENDING | SENT | FAILED"
        UUID order_id
        TIMESTAMP sent_at
        TIMESTAMP created_at
    }
```

## Key Design Decisions

1. **UUIDs as primary keys** for orders, products, payments, and users. Prevents enumeration and is safe for distributed systems.
2. **Denormalized product info in order items** (product_name, product_image, unit_price). This ensures order history is accurate even if product details change later.
3. **Idempotency key on payments** prevents duplicate webhook processing.
4. **Indexes** on foreign keys and commonly queried columns (order_id, user_id, external_payment_id).
5. **No cross-database foreign keys** - referential integrity between services is maintained at the application level.

## Database Initialization

The `init-multiple-dbs.sh` script creates all five databases on first PostgreSQL startup. It is mounted into the Docker entrypoint directory.

```bash
# Databases created:
auth_db, catalog_db, order_db, payment_db, notification_db
```

## Connection Configuration

Each service connects to its own database via environment variables:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${DB_NAME}
    username: ${POSTGRES_USER:cronos}
    password: ${POSTGRES_PASSWORD:cronos_secret_2024}
```
