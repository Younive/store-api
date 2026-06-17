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
| Monitoring | Sentry 8.43.1 (errors + optional tracing) |
| Mapping | MapStruct 1.6.3 |
| Docs | SpringDoc OpenAPI |
| Build | Maven |
| Testing | JUnit 5, Mockito, MockMvc, Playwright (E2E) |

## Prerequisites

- Java 23+
- MySQL 8+
- Stripe account (for payment features)

## Setup

**1. Set environment variables**

No credentials live in config files — everything comes from the environment. See [.env.example](.env.example) for the full template (copy to `.env`, which is gitignored):

```bash
MYSQL_USER=root                      # dev datasource user (defaults to root)
MYSQL_PASSWORD=your_db_password      # dev datasource password
JWT_SECRET=your_secret               # HS256 key, must be at least 32 chars
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET_KEY=whsec_...

# Optional — Sentry error monitoring (leave unset to disable the SDK entirely)
SENTRY_DSN=https://...@o0.ingest.sentry.io/0
SENTRY_ENVIRONMENT=production            # defaults to "development"
SENTRY_TRACES_SAMPLE_RATE=0.1            # performance tracing, defaults to 0.0 (off)

# Optional — admin user seeding (leave unset to skip)
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=your_admin_password
```

The dev datasource points at `jdbc:mysql://localhost:3306/store_api` (auto-created). Adjust the URL in `src/main/resources/application-dev.yaml` if your MySQL lives elsewhere.

**2. Run**

```bash
./mvnw spring-boot:run
```

Flyway auto-creates the schema on first run. On first startup, if `ADMIN_EMAIL` and `ADMIN_PASSWORD` are set, an admin user is seeded automatically via `DataInitializer`.

## Deployment

The app ships as a Docker image and deploys to Railway via GitHub Actions.

**Docker**

Multi-stage build — `eclipse-temurin:23-jdk-alpine` for compilation, `eclipse-temurin:23-jre-alpine` for runtime. Build:

```bash
docker build -t store-api .
```

**Railway**

Required environment variables on the Railway service:

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://${{MYSQLHOST}}:${{MYSQLPORT}}/${{MYSQLDATABASE}}` |
| `SPRING_DATASOURCE_USERNAME` | `${{MYSQLUSER}}` |
| `SPRING_DATASOURCE_PASSWORD` | `${{MYSQLPASSWORD}}` |
| `JWT_SECRET` | strong random 32+ char string |
| `STRIPE_SECRET_KEY` | `sk_live_...` |
| `STRIPE_WEBHOOK_SECRET_KEY` | `whsec_...` |
| `SENTRY_DSN` | prod DSN |
| `SENTRY_ENVIRONMENT` | `production` |
| `ADMIN_EMAIL` | seed admin email |
| `ADMIN_PASSWORD` | seed admin password |

**CI/CD (GitHub Actions)**

`.github/workflows/ci.yml` — two jobs:
- `test`: runs on every push and PR; spins up a MySQL 8 service container and runs the full test suite
- `deploy`: triggers on merge to `main` after tests pass; deploys to Railway via `RAILWAY_TOKEN` secret

## Testing

```bash
./mvnw test          # unit + MockMvc integration tests (needs MySQL)
./mvnw test -Pe2e    # E2E suite (Playwright) — needs the app running on :8080
```

The E2E suite lives in `src/test/java/com/younive/store/e2e/` and is excluded from the default `test` run. See [docs/e2e.md](docs/e2e.md) for prerequisites and configuration.

## Error Monitoring (Sentry)

Set `SENTRY_DSN` and the SDK activates automatically — no code changes needed. Captured out of the box:

- Unhandled exceptions from any controller (via the auto-registered Spring MVC exception resolver)
- Anything logged at `ERROR` with a throwable (e.g. Stripe checkout-session failures) via the Logback appender

Client-error responses handled by `GlobalExceptionHandler` (validation 400s, etc.) are intentionally not reported. `send-default-pii` is off, so request bodies/cookies/user identifiers are not attached to events. With `SENTRY_DSN` unset (local dev, tests, CI) the SDK is fully disabled.

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
{ "token": "eyJ..." }
```

Token lifetimes: access = 5 min, refresh = 7 days. Tokens carry a `type` claim (`access`/`refresh`); the refresh endpoint rejects access tokens and the auth filter rejects refresh tokens.

---

### Users — `/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/users` | Public | Register new user (always role `USER`) |
| `GET` | `/users` | ADMIN | List all users (optional `?sort=name\|email`) |
| `GET` | `/users/{id}` | Bearer | Get user by ID |
| `PUT` | `/users/{id}` | Self or ADMIN | Update user |
| `DELETE` | `/users/{id}` | Self or ADMIN | Delete user |
| `POST` | `/users/{id}/change-password` | Self or ADMIN | Change password (verifies old password) |

---

### Products — `/products`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/products` | Bearer | List all products (optional `?categoryId=`) |
| `GET` | `/products/{id}` | Bearer | Get product by ID |
| `POST` | `/products` | ADMIN | Create product |
| `PUT` | `/products/{id}` | ADMIN | Update product |
| `DELETE` | `/products/{id}` | ADMIN | Delete product |

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
| `POST` | `/checkout/webhook` | Public | Stripe webhook handler (signature verified; forged/missing signature → 400) |

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
| V7 | Seed data (4 categories, 12 products) |

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
└── common/      # GlobalExceptionHandler, LoggingFilter, FlywayConfig, DataInitializer, ErrorDto
```

## Roles

| Role | Access |
|---|---|
| `USER` | Standard authenticated endpoints; can only modify their own account |
| `ADMIN` | All endpoints including `/admin/*`, product writes, user listing |

Registration always assigns `USER`. An admin is seeded on first startup when `ADMIN_EMAIL` + `ADMIN_PASSWORD` env vars are set. To promote an existing user manually:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';
```
