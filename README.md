# Cronos Geneve - Swiss Luxury Timepieces

A premium e-commerce platform for luxury Swiss watches, built with a microservices architecture using Java 21, Spring Boot 3.3, React, and PostgreSQL.

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│   Frontend   │────▶│  API Gateway  │────▶│  Auth Service    │
│   (React)    │     │  (Port 8080)  │     │  (Port 8081)     │
│  Port 5173   │     │               │     └─────────────────┘
└─────────────┘     │               │     ┌─────────────────┐
                     │               │────▶│ Catalog Service  │
                     │               │     │  (Port 8082)     │
                     │               │     └─────────────────┘
                     │               │     ┌─────────────────┐
                     │               │────▶│  Order Service   │
                     │               │     │  (Port 8083)     │
                     │               │     └─────────────────┘
                     │               │     ┌─────────────────┐
                     │               │────▶│ Payment Service  │
                     │               │     │  (Port 8084)     │
                     │               │     └─────────────────┘
                     │               │     ┌─────────────────┐
                     │               │────▶│Notification Svc  │
                     └──────────────┘     │  (Port 8085)     │
                                          └─────────────────┘
                                                   │
                                          ┌─────────────────┐
                                          │   PostgreSQL     │
                                          │  (Port 5432)     │
                                          │  5 databases     │
                                          └─────────────────┘
```

## Tech Stack

### Backend
- Java 21
- Spring Boot 3.3.5
- Spring Cloud Gateway 2023.0.3
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data JPA
- PostgreSQL 16
- Flyway migrations
- Maven

### Frontend
- React 19
- Vite
- Tailwind CSS 4
- React Router
- Axios
- Lucide React icons
- Sonner notifications

### Infrastructure
- Docker & Docker Compose
- Nginx (frontend production)

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 22+
- Docker & Docker Compose
- PostgreSQL 16 (or use Docker)

## Quick Start with Docker

```bash
# Clone and enter the project
cd cronos-geneve-java

# Copy environment variables
cp .env.example .env

# Start all services
docker compose up --build
```

Access the application:
- Frontend: http://localhost:5173
- API Gateway: http://localhost:8080
- Auth Service: http://localhost:8081
- Catalog Service: http://localhost:8082
- Order Service: http://localhost:8083
- Payment Service: http://localhost:8084
- Notification Service: http://localhost:8085

## Local Development

### Backend Services

Each service can be run independently. First start PostgreSQL:

```bash
docker compose up postgres -d
```

Then run any service:

```bash
cd backend/auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Demo Accounts

| Role  | Email              | Password   |
|-------|--------------------|------------|
| Admin | admin@cronos.com   | Admin123!  |
| User  | user@cronos.com    | User123!   |

## API Endpoints

### Auth Service (`/api/auth`)
| Method | Endpoint       | Description        | Auth |
|--------|----------------|--------------------|------|
| POST   | /register      | Register new user  | No   |
| POST   | /login         | Login              | No   |
| POST   | /refresh       | Refresh token      | No   |
| GET    | /profile       | Get user profile   | Yes  |
| GET    | /validate      | Validate JWT token | Yes  |

### Catalog Service (`/api/products`, `/api/brands`, `/api/categories`)
| Method | Endpoint              | Description          | Auth  |
|--------|-----------------------|----------------------|-------|
| GET    | /products             | List products        | No    |
| GET    | /products/{id}        | Get product          | No    |
| GET    | /products/featured    | Featured products    | No    |
| GET    | /products/search      | Search products      | No    |
| POST   | /products             | Create product       | Admin |
| PUT    | /products/{id}        | Update product       | Admin |
| PATCH  | /products/{id}/stock  | Update stock         | Admin |
| GET    | /brands               | List brands          | No    |
| GET    | /categories           | List categories      | No    |

### Order Service (`/api/orders`)
| Method | Endpoint            | Description         | Auth |
|--------|---------------------|---------------------|------|
| POST   | /orders             | Create order        | Yes  |
| GET    | /orders             | List user orders    | Yes  |
| GET    | /orders/{id}        | Get order           | Yes  |
| PUT    | /orders/{id}/status | Update status       | Int  |
| POST   | /orders/{id}/cancel | Cancel order        | Yes  |

### Payment Service (`/api/payments`)
| Method | Endpoint                | Description            | Auth |
|--------|-------------------------|------------------------|------|
| POST   | /payments/preference    | Create MP preference   | Yes  |
| POST   | /payments/webhook       | MP webhook             | No   |
| GET    | /payments/order/{id}    | Get payment by order   | Yes  |
| GET    | /payments/{id}          | Get payment            | Yes  |

## Environment Variables

See `.env.example` for all available configuration options.

Key variables:
- `JWT_SECRET` - JWT signing key (min 256 bits)
- `MERCADOPAGO_ACCESS_TOKEN` - Mercado Pago sandbox token
- `MERCADOPAGO_WEBHOOK_SECRET` - Webhook HMAC secret
- `POSTGRES_PASSWORD` - Database password

## Testing

```bash
# Run all tests
cd backend/auth-service && mvn test
cd backend/catalog-service && mvn test
cd backend/order-service && mvn test
cd backend/payment-service && mvn test
cd backend/notification-service && mvn test
```

## Project Structure

```
cronos-geneve-java/
├── frontend/                 # React + Vite frontend
├── backend/
│   ├── api-gateway/         # Spring Cloud Gateway
│   ├── auth-service/        # Authentication & authorization
│   ├── catalog-service/     # Products, brands, categories
│   ├── order-service/       # Order management
│   ├── payment-service/     # Mercado Pago integration
│   └── notification-service/# Email notifications
├── infrastructure/
│   ├── docker/              # Docker configs
│   └── database/            # DB init scripts
├── docs/                    # Documentation
├── docker-compose.yml
├── .env.example
└── README.md
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Microservices](docs/MICROSERVICES.md)
- [Database](docs/DATABASE.md)
- [Security](docs/SECURITY.md)
- [Payments](docs/PAYMENTS.md)
- [API Reference](docs/API.md)
- [Deployment](docs/DEPLOYMENT.md)
- [Progress](docs/PROGRESS.md)
