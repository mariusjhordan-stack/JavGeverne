# API Reference

Base URL: `http://localhost:8080` (via API Gateway)

## Authentication

Most endpoints require a JWT token in the `Authorization` header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## Auth Service

### Register

```
POST /api/auth/register
```

**Request:**
```json
{
  "email": "john@example.com",
  "password": "SecurePass123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response (201):**
```json
{
  "token": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "email": "john@example.com",
  "role": "USER",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Errors:**
- `409` - Email already registered
- `400` - Validation error

### Login

```
POST /api/auth/login
```

**Request:**
```json
{
  "email": "john@example.com",
  "password": "SecurePass123"
}
```

**Response (200):** Same as register response.

**Errors:**
- `401` - Invalid credentials

### Refresh Token

```
POST /api/auth/refresh
```

**Request:**
```json
{
  "refreshToken": "eyJhbGci..."
}
```

**Response (200):** Same as login response with new tokens.

### Get Profile

```
GET /api/auth/profile
Authorization: Bearer {token}
```

**Response (200):**
```json
{
  "id": "uuid",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "createdAt": "2024-01-01T00:00:00"
}
```

---

## Catalog Service

### List Products

```
GET /api/products?page=0&size=12&sort=price,asc&brandId=1&categoryId=2&minPrice=1000&maxPrice=10000&movementType=AUTOMATIC&q=submariner
```

**Response (200):**
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Submariner Date",
      "description": "...",
      "referenceNumber": "126610LN",
      "brandId": 1,
      "brandName": "Rolex",
      "categoryId": 3,
      "categoryName": "Dive",
      "price": 9950.00,
      "currency": "USD",
      "stock": 5,
      "caseMaterial": "Oystersteel",
      "caseDiameter": "41mm",
      "movementType": "AUTOMATIC",
      "waterResistance": "300m",
      "crystalType": "Sapphire",
      "dialColor": "Black",
      "strapMaterial": "Oystersteel bracelet",
      "powerReserve": "70 hours",
      "imageUrl": "https://...",
      "galleryUrls": "...",
      "featured": true,
      "active": true,
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ],
  "totalElements": 12,
  "totalPages": 1,
  "size": 12,
  "number": 0
}
```

### Get Product

```
GET /api/products/{id}
```

**Response (200):** Single product object (same structure as above).

**Errors:**
- `404` - Product not found

### Get Featured Products

```
GET /api/products/featured
```

**Response (200):** Array of featured product objects.

### Search Products

```
GET /api/products/search?q=omega
```

**Response (200):** Paginated product list matching the search query.

### Create Product (Admin)

```
POST /api/products
Authorization: Bearer {admin-token}
```

**Request:**
```json
{
  "name": "New Watch",
  "description": "Description",
  "referenceNumber": "REF001",
  "brandId": 1,
  "categoryId": 1,
  "price": 5000.00,
  "stock": 10,
  "caseMaterial": "Steel",
  "movementType": "AUTOMATIC",
  "imageUrl": "https://...",
  "featured": false,
  "active": true
}
```

### List Brands

```
GET /api/brands
```

**Response (200):**
```json
[
  {
    "id": 1,
    "name": "Rolex",
    "description": "...",
    "logoUrl": "...",
    "country": "Switzerland"
  }
]
```

### List Categories

```
GET /api/categories
```

**Response (200):**
```json
[
  {
    "id": 1,
    "name": "Sport",
    "description": "...",
    "slug": "sport"
  }
]
```

---

## Order Service

### Create Order

```
POST /api/orders
Authorization: Bearer {token}
```

**Request:**
```json
{
  "items": [
    {
      "productId": "uuid",
      "productName": "Rolex Submariner",
      "productImage": "https://...",
      "quantity": 1,
      "unitPrice": 9950.00
    }
  ],
  "shippingAddress": "123 Main St, Zurich, Switzerland"
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "userEmail": "user@example.com",
  "status": "PENDING",
  "subtotal": 9950.00,
  "tax": 0.00,
  "total": 9950.00,
  "currency": "USD",
  "shippingAddress": "123 Main St, Zurich, Switzerland",
  "items": [...],
  "createdAt": "2024-01-01T00:00:00"
}
```

### List User Orders

```
GET /api/orders
Authorization: Bearer {token}
```

**Response (200):** Array of order objects.

### Get Order

```
GET /api/orders/{id}
Authorization: Bearer {token}
```

### Cancel Order

```
POST /api/orders/{id}/cancel
Authorization: Bearer {token}
```

**Response (200):** Updated order with status `CANCELLED`.

---

## Payment Service

### Create Preference

```
POST /api/payments/preference
Authorization: Bearer {token}
```

**Request:**
```json
{
  "orderId": "uuid",
  "items": [
    {
      "title": "Rolex Submariner Date",
      "quantity": 1,
      "unitPrice": 9950.00
    }
  ],
  "payerEmail": "user@example.com",
  "totalAmount": 9950.00,
  "currency": "USD"
}
```

**Response (200):**
```json
{
  "preferenceId": "mp-preference-id",
  "initPoint": "https://www.mercadopago.com/checkout/v1/redirect?pref_id=...",
  "sandboxInitPoint": "https://sandbox.mercadopago.com/checkout/v1/redirect?pref_id=..."
}
```

### Webhook (Mercado Pago)

```
POST /api/payments/webhook
x-signature: ts=...,v1=...
x-request-id: ...
```

**Request (from MP):**
```json
{
  "action": "payment.created",
  "type": "payment",
  "data": {
    "id": "12345678"
  }
}
```

**Response:** `200 OK` (always, to acknowledge receipt)

### Get Payment by Order

```
GET /api/payments/order/{orderId}
Authorization: Bearer {token}
```

---

## Error Responses

All error responses follow a consistent format:

```json
{
  "timestamp": "2024-01-01T00:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register"
}
```

### Common HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (missing/invalid token) |
| 403 | Forbidden (insufficient role) |
| 404 | Not Found |
| 409 | Conflict (duplicate resource) |
| 500 | Internal Server Error |
