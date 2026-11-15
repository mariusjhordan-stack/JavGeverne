# Deployment Guide

## Local Development with Docker Compose

### Prerequisites

- Docker 24+
- Docker Compose v2+

### Quick Start

```bash
cp .env.example .env
# Edit .env with your settings
docker compose up --build
```

### Service URLs

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| Auth Service | http://localhost:8081 |
| Catalog Service | http://localhost:8082 |
| Order Service | http://localhost:8083 |
| Payment Service | http://localhost:8084 |
| Notification Service | http://localhost:8085 |
| PostgreSQL | localhost:5432 |

### Health Checks

Each service exposes a health endpoint:

```bash
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # Catalog
curl http://localhost:8083/actuator/health  # Order
curl http://localhost:8084/actuator/health  # Payment
curl http://localhost:8085/actuator/health  # Notification
```

### Stopping Services

```bash
docker compose down          # Stop containers
docker compose down -v       # Stop and remove volumes (deletes data)
```

## Running Without Docker

### 1. Start PostgreSQL

```bash
docker compose up postgres -d
```

Or install PostgreSQL locally and create the databases manually:

```sql
CREATE DATABASE auth_db;
CREATE DATABASE catalog_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE notification_db;
```

### 2. Start Backend Services

```bash
# Each in a separate terminal
cd backend/auth-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd backend/catalog-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd backend/order-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd backend/payment-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd backend/notification-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd backend/api-gateway && mvn spring-boot:run
```

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

## Production Deployment

### Environment Variables

Critical variables for production:

| Variable | Description | Required |
|----------|-------------|----------|
| `JWT_SECRET` | Strong random key, min 256 bits | Yes |
| `POSTGRES_PASSWORD` | Strong database password | Yes |
| `MERCADOPAGO_ACCESS_TOKEN` | Production access token | For payments |
| `MERCADOPAGO_WEBHOOK_SECRET` | Webhook signing secret | For payments |
| `MERCADOPAGO_NOTIFICATION_URL` | Public webhook URL | For payments |
| `ALLOWED_ORIGINS` | Frontend production URL | Yes |

### HTTPS

HTTPS should be terminated at the load balancer or reverse proxy level. The application services run on HTTP internally.

Example with Nginx as reverse proxy:

```nginx
server {
    listen 443 ssl;
    server_name cronos-geneve.com;

    ssl_certificate /etc/ssl/certs/cronos.crt;
    ssl_certificate_key /etc/ssl/private/cronos.key;

    location / {
        proxy_pass http://frontend:80;
    }

    location /api/ {
        proxy_pass http://api-gateway:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Database

For production:
- Use managed PostgreSQL (AWS RDS, Google Cloud SQL, etc.)
- Enable SSL connections
- Set up automated backups
- Use separate database instances per service if needed for scaling
- Configure connection pooling (HikariCP is included with Spring Boot)

### Production Checklist

- [ ] Set strong `JWT_SECRET` (at least 256 bits of entropy)
- [ ] Set strong `POSTGRES_PASSWORD`
- [ ] Configure `ALLOWED_ORIGINS` to production frontend URL only
- [ ] Set up HTTPS termination
- [ ] Configure Mercado Pago production credentials
- [ ] Set `MERCADOPAGO_NOTIFICATION_URL` to public HTTPS webhook URL
- [ ] Remove or restrict demo accounts
- [ ] Enable PostgreSQL SSL
- [ ] Set up database backups
- [ ] Configure log aggregation
- [ ] Set up monitoring and alerting
- [ ] Review and restrict CORS origins
- [ ] Run `spring.profiles.active=prod` on all services

### Monitoring

Each service exposes Spring Boot Actuator endpoints:

- `/actuator/health` - Service health status
- `/actuator/info` - Application info
- `/actuator/metrics` - Micrometer metrics (when enabled)

For production monitoring, integrate with:
- Prometheus + Grafana for metrics
- ELK stack or similar for log aggregation
- Uptime monitoring for health endpoints

### Scaling

Each microservice can be scaled independently:

```bash
docker compose up --scale catalog-service=3
```

When scaling, consider:
- Load balancing at the gateway level
- Database connection pool limits
- Shared nothing architecture (each instance is stateless)
