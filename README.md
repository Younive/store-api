# Store API

REST API for an e-commerce store. Built with Spring Boot 4, Java 23, MySQL, and Stripe.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.3 |
| Language | Java 23 |
| Database | MySQL 8 + Flyway migrations |
| Auth | Spring Security + JWT (JJWT 0.12.6) |
| Payments | Stripe Java SDK 32.1 |
| Mapping | MapStruct 1.7 |
| Docs | SpringDoc OpenAPI |
| Build | Maven |

## Prerequisites

- Java 23+
- MySQL 8+
- Stripe account (for payment features)

## Setup

**1. Clone and configure**

Update DB credentials in `src/main/resources/application-dev.yaml` if needed:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/store_api?createDatabaseIfNotExist=true
    username: root
    password: your_password
websiteUrl: http://localhost:4242
```

**2. Set environment variables**

```bash
JWT_SECRET=your_256bit_secret
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET_KEY=whsec_...
```

**3. Run**

```bash
./mvnw spring-boot:run
```

Flyway auto-creates the schema on first run.

## API Reference

### Auth — `/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/login` | Public | Login. Returns access token in body, refresh token as HttpOnly cookie |
| `POST` | `/auth/refresh` | Cookie | Refresh access token using `refreshToken` cookie |
| `GET` | `/auth/me` | Bearer | Get current authenticated user |

**Login request:**
```json
{ "email": "user@example.com", "password": "secret" }
```

**Login response:**
```json
{ "accessToken": "eyJ..." }
```

Token lifetimes: access = 5 min, refresh = 7 days.

---

### Users — `/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/users` | Public | Register new user |
| `GET` | `/users` | Bearer | List all users |
| `GET` | `/users/{id}` | Bearer | Get user by ID |
| `PUT` | `/users/{id}` | Bearer | Update user |
| `DELETE` | `/users/{id}` | Bearer | Delete user |
| `POST` | `/users/{id}/change-password` | Bearer | Change password |

---

### Products — `/products`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/products` | Public | List all products (optional `?categoryId=`) |
| `GET` | `/products/{id}` | Public | Get product by ID |
| `POST` | `/products` | Bearer | Create product |
| `PUT` | `/products/{id}` | Bearer | Update product |
| `DELETE` | `/products/{id}` | Bearer | Delete product |

---

### Carts — `/carts`

Cart is anonymous and identified by a UUID.

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/carts` | Public | Create new cart |
| `GET` | `/carts/{cartId}` | Public | Get cart |
| `POST` | `/carts/{cartId}/items` | Public | Add item to cart |
| `PUT` | `/carts/{cartId}/items/{productId}` | Public | Update item quantity |
| `DELETE` | `/carts/{cartId}/items/{productId}` | Public | Remove item |
| `DELETE` | `/carts/{cartId}/items` | Public | Clear cart |

---

### Checkout & Payments — `/checkout`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/checkout` | Bearer | Create Stripe checkout session |
| `POST` | `/checkout/webhook` | Public | Stripe webhook handler |

**Checkout request:**
```json
{ "cartId": "uuid-here" }
```

Webhook updates order `PaymentStatus` to `PAID` or `FAILED`.

---

### Orders — `/orders`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/orders` | Bearer | List current user's orders |
| `GET` | `/orders/{orderId}` | Bearer | Get order by ID |

---

### Admin — `/admin`

Requires `ADMIN` role.

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/hello` | Health check for admin role |

## Authentication Flow

```
POST /auth/login
  → access token (Bearer, 5 min)
  → refreshToken cookie (HttpOnly, 7 days)

Authorization: Bearer <accessToken>

POST /auth/refresh  (cookie sent automatically)
  → new access token
```

## Database Schema

Managed by Flyway. Migrations in `src/main/resources/db/migration/`:

| Version | Description |
|---|---|
| V1 | Initial schema (users, products, categories) |
| V2 | Carts table |
| V3 | Cart items table |
| V4 | Role column on users |
| V5 | Orders table |
| V6 | Order items table |

## Project Structure

```
src/main/java/com/younive/store/
├── auth/        # JWT login, refresh, SecurityConfig
├── users/       # Registration, profile, address, roles
├── products/    # Product & category CRUD
├── carts/       # UUID-based shopping cart
├── orders/      # Order management, PaymentStatus
├── payments/    # Stripe integration, webhook
├── admin/       # Admin-only endpoints
└── common/      # GlobalExceptionHandler, LoggingFilter, ErrorDto
```

## Roles

| Role | Access |
|---|---|
| `USER` | Standard authenticated endpoints |
| `ADMIN` | All endpoints including `/admin/*` |
