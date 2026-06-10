# End-to-End (E2E) Tests

This project has a Playwright-for-Java E2E suite that exercises the REST API of a **running**
server. Because the app is an API (the only HTML view is a trivial Thymeleaf index), the tests
drive endpoints through Playwright's `APIRequestContext` rather than a browser. A single
browser-based smoke test confirms the app is reachable.

- Location: `src/test/java/com/younive/store/e2e/`
- Package: `com.younive.store.e2e`
- All classes are tagged `@Tag("e2e")` and named `*E2ETest`.

## What is covered

| Class | Area |
|-------|------|
| `AuthE2ETest` | login (success / bad password / unknown user), `GET /auth/me`, refresh rejection |
| `UserE2ETest` | registration, duplicate email, validation, get-by-id, admin-only listing, self-vs-admin update / change-password |
| `ProductE2ETest` | admin create/update/delete, authenticated reads, 401/403/404 boundaries |
| `CartE2ETest` | anonymous cart create, add/update/remove/clear items, missing product/cart errors |
| `OrderE2ETest` | auth required, owner-only, empty list, 404 |
| `CheckoutE2ETest` | unauthenticated 401, unknown cart 400, Stripe 500 error contract, forged webhook rejected |
| `HomePageSmokeE2ETest` | one real-browser smoke test: home page renders the greeting |

## Isolation from the existing test suite

The normal `./mvnw.cmd test` run **excludes** everything under `**/e2e/**` (configured on the
Surefire plugin in `pom.xml`), so the existing JUnit unit + MockMvc integration tests are
unaffected and do **not** require a running server. The E2E suite runs only under the dedicated
`e2e` Maven profile.

## Prerequisites

1. **MySQL** running on `localhost:3306` (database `store_api`; auto-created on app start).
2. **The app must be running** on the base URL you test against (default `http://localhost:8080`).
3. **Playwright browsers installed** (only needed for the browser smoke test):

   ```bash
   ./mvnw.cmd dependency:build-classpath "-Dmdep.outputFile=target/e2e-cp.txt" "-Dmdep.includeScope=test"
   # then, using that classpath (Windows ; separator), plus target/classes;target/test-classes:
   java -cp "<contents of target/e2e-cp.txt>;target/classes;target/test-classes" com.microsoft.playwright.CLI install chromium
   ```

   The Playwright-documented one-liner also works if you add the `exec-maven-plugin`:

   ```bash
   mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps"
   ```

## Running

### 1. Start the app (separate terminal)

Required env vars (dummy values are fine for everything except a real Stripe charge):

```powershell
$env:MYSQL_USER='root'
$env:MYSQL_PASSWORD='<your-mysql-password>'
$env:JWT_SECRET='a-secret-of-at-least-32-characters-xxxxxxxx'
$env:STRIPE_SECRET_KEY='sk_test_dummy'
$env:STRIPE_WEBHOOK_SECRET_KEY='whsec_dummy'
.\mvnw.cmd spring-boot:run
```

### 2. Run the E2E suite (another terminal)

```powershell
.\mvnw.cmd test -Pe2e `
  "-DbaseUrl=http://localhost:8080" `
  "-De2e.mysql.url=jdbc:mysql://localhost:3306/store_api" `
  "-De2e.mysql.user=root" `
  "-De2e.mysql.password=<your-mysql-password>"
```

## Configuration (system properties)

All read in `E2EConfig`; override with `-D…`:

| Property | Default | Purpose |
|----------|---------|---------|
| `baseUrl` | `http://localhost:8080` | Base URL of the running app |
| `e2e.mysql.url` | `jdbc:mysql://localhost:3306/store_api` | JDBC URL used only to promote a test user to ADMIN |
| `e2e.mysql.user` | `MYSQL_USER` env, else `root` | MySQL user |
| `e2e.mysql.password` | `MYSQL_PASSWORD` env, else empty | MySQL password |
| `headed` | `false` | Set `-Dheaded=true` to run the browser smoke test headed |

Credentials are never hardcoded — supply them via `-D` or the `MYSQL_*` env vars.

## How admin tests work (no seeded admin)

The app seeds no admin and registration always assigns role `USER`. The admin-flow tests therefore
register a normal user through the public API, then promote it to `ADMIN` with a direct JDBC
`UPDATE users SET role='ADMIN'` (see `AdminSupport`). This is why the `e2e.mysql.*` properties are
required for `ProductE2ETest`, `CartE2ETest`, and `CheckoutE2ETest`.

## Notes on observed behavior (assertions reflect the real app)

The security config permits the `ERROR` dispatcher, so error statuses produced inside `permitAll`
endpoints reach the client unmasked:

- `POST /auth/refresh` with a missing `refreshToken` cookie → **400**; with an access-type token
  in the cookie → **401** (`BadCredentialsException` handler).
- `POST /checkout/webhook` with a forged or missing `Stripe-Signature` → **400** (Stripe will not
  retry rejected webhooks).
- `GET /` (Thymeleaf index) is permitAll and renders the greeting anonymously → **200**.

`POST /checkout` of a valid cart returns **500** with body
`{"error":"Error creating a checkout session"}` because the dummy Stripe key makes the real Stripe
call fail; the test asserts that error contract rather than a happy path.

## Test data

Emails are generated per run (`e2e-<uuid>@example.com`) so reruns don't collide with rows already
persisted in MySQL. The suite does not clean up created rows; this is expected for E2E against a
shared dev database.
