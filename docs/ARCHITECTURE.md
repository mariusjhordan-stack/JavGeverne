# Architecture Overview

## System Architecture

Cronos Geneve follows a microservices architecture with an API Gateway as the single entry point for all client requests.

```mermaid
graph TB
    Client[React Frontend<br/>Port 5173]
    Gateway[API Gateway<br/>Spring Cloud Gateway<br/>Port 8080]
    Auth[Auth Service<br/>Port 8081]
    Catalog[Catalog Service<br/>Port 8082]
    Order[Order Service<br/>Port 8083]
    Payment[Payment Service<br/>Port 8084]
    Notification[Notification Service<br/>Port 8085]
    AuthDB[(auth_db)]
    CatalogDB[(catalog_db)]
    OrderDB[(order_db)]
    PaymentDB[(payment_db)]
    NotificationDB[(notification_db)]
    MP[Mercado Pago API]

    Client -->|HTTP| Gateway
    Gateway -->|/api/auth/**| Auth
    Gateway -->|/api/products/**<br/>/api/brands/**<br/>/api/categories/**| Catalog
    Gateway -->|/api/orders/**| Order
    Gateway -->|/api/payments/**| Payment
    Gateway -->|/api/notifications/**| Notification

    Auth --> AuthDB
    Catalog --> CatalogDB
    Order --> OrderDB
    Order -->|Stock reservation| Catalog
    Payment --> PaymentDB
    Payment -->|Order status update| Order
    Payment -->|Checkout Pro| MP
    Notification --> NotificationDB

    MP -->|Webhook| Payment
```

## Service Communication

Services communicate via synchronous REST calls. Each service owns its data and exposes an HTTP API.

| From | To | Purpose |
|------|----|---------|
| Frontend | API Gateway | All client requests |
| API Gateway | All services | Route requests |
| Order Service | Catalog Service | Reserve/release stock |
| Payment Service | Order Service | Update order status after payment |
| Payment Service | Mercado Pago | Create preferences, verify payments |
| Mercado Pago | Payment Service | Webhook notifications |

## Request Flows

### Authentication Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant G as Gateway
    participant A as Auth Service
    participant DB as auth_db

    C->>G: POST /api/auth/login
    G->>A: Forward request
    A->>DB: Find user by email
    DB-->>A: User record
    A->>A: Verify BCrypt password
    A->>A: Generate JWT + Refresh Token
    A-->>G: AuthResponse (token, user info)
    G-->>C: 200 OK

    Note over C: Subsequent requests include<br/>Authorization: Bearer {token}
```

### Order + Payment Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant G as Gateway
    participant O as Order Service
    participant Cat as Catalog Service
    participant P as Payment Service
    participant MP as Mercado Pago

    C->>G: POST /api/orders
    G->>O: Create order
    O->>Cat: Reserve stock (PATCH /products/{id}/stock)
    Cat-->>O: Stock reserved
    O-->>G: Order created (PENDING)
    G-->>C: Order response

    C->>G: POST /api/payments/preference
    G->>P: Create preference
    P->>MP: POST /checkout/preferences
    MP-->>P: Preference with init_point URL
    P-->>G: PreferenceResponse
    G-->>C: Redirect to Mercado Pago checkout

    Note over C,MP: User completes payment on Mercado Pago

    MP->>P: POST /api/payments/webhook
    P->>P: Validate HMAC signature
    P->>MP: GET /v1/payments/{id}
    MP-->>P: Payment details
    P->>P: Validate amount, currency, order
    P->>O: PUT /orders/{id}/status (PAID)
    O-->>P: Updated
    P->>P: Save payment record
```

## Technology Choices

| Technology | Choice | Rationale |
|------------|--------|-----------|
| Language | Java 21 | LTS, virtual threads, pattern matching |
| Framework | Spring Boot 3.3 | Mature ecosystem, production-ready |
| Gateway | Spring Cloud Gateway | Native Spring integration, reactive |
| Auth | JWT + Spring Security | Stateless, scalable, industry standard |
| Database | PostgreSQL 16 | Robust, JSON support, mature |
| Migrations | Flyway | Version-controlled schema management |
| Frontend | React + Vite | Fast development, rich ecosystem |
| Styling | Tailwind CSS | Utility-first, rapid UI development |
| Containers | Docker Compose | Reproducible local environments |

## Design Principles

1. **Database per Service** - Each microservice owns its data store, preventing tight coupling.
2. **API Gateway Pattern** - Single entry point simplifies client integration and enables cross-cutting concerns.
3. **Stateless Authentication** - JWT tokens enable horizontal scaling without session affinity.
4. **Synchronous Communication** - REST calls between services for simplicity and debuggability.
5. **Defense in Depth** - Validation at every layer: gateway, service, database constraints.
