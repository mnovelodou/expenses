# Architecture

A short tour of how Expenses is built and why.

## Stack

| Concern            | Choice                                              |
| ------------------ | --------------------------------------------------- |
| Language / runtime | Java 21                                             |
| Framework          | Spring Boot 3.5 (`spring-boot-starter-web`)         |
| Persistence        | PostgreSQL via Spring JDBC (`JdbcTemplate`) — no ORM |
| Auth               | OAuth2 resource server (JWT bearer tokens)          |
| Build              | Gradle                                              |
| Testing            | JUnit 5, Spring Security Test, Testcontainers, MockWebServer |

## Why these choices

- **Plain JDBC, no JPA/Hibernate.** SQL is hand-written and lives close to the
  repositories. This keeps query behavior explicit and predictable — important
  for things like cursor pagination and the gap calculation, where I want to
  control exactly what hits the database. There is no entity graph or lazy
  loading to reason about.
- **OAuth2 resource server.** The API trusts signed JWTs and derives the caller
  identity from the token rather than from request fields. Ownership is enforced
  server-side so a user can only see and mutate their own accounts and expenses.
  The service only *validates* tokens — it is provider-agnostic and issues
  nothing itself (see [Authentication](#authentication)).
- **DTO `value`-wrapping.** Request and response bodies wrap their payload in a
  `value` field (e.g. `CreateAccountRequest(value)`), giving a stable envelope to
  evolve around.

## Module layout

Source lives under `src/main/java/com/novelosoftware/expenses`:

```
controllers/   REST endpoints (AccountController, ExpenseController)
services/      Business rules and orchestration
repositories/  JdbcTemplate-backed data access
entities/      Database row representations
dto/           API request/response records + enums (Category, SubCategory, AccountType)
mappers/       Entity <-> DTO translation
security/      SecurityConfig, CurrentUser (caller identity from the JWT)
exceptions/    Domain exceptions + GlobalExceptionHandler
util/          Cursor encoding, date-window resolution
```

Database schema is defined in
[`src/main/resources/db/schema.sql`](../src/main/resources/db/schema.sql).

## Key domain concepts

- **Accounts** have a type (debit/credit), currency, an `initialAmount`, a
  `currentAmount`, and a `periodStart`.
- **Gap** is the unexplained balance delta for an account:
  `currentAmount - initialAmount - SUM(expenses since periodStart)`. It's only
  computed on request (`?includeGap=true`) and surfaces money the recorded
  expenses don't account for.
- **Expenses** carry an `amount` plus a `transactionAmount` — the original
  pre-split amount. Splitting one charge into several categorized lines is a
  first-class idea, and a valid split's line amounts sum back to the original
  transaction amount.
- **Categories / sub-categories.** Expenses are tagged by `SubCategory`, from
  which the broader `Category` is inferred.

## Authentication

The app is an OAuth2 **resource server** and nothing more — it validates incoming
JWT access tokens but never issues, stores, or refreshes them. That keeps it
independent of any single identity provider: point it at whichever OAuth2 / OIDC
provider you run, and obtain tokens however that provider prefers (device flow,
authorization code, client credentials, etc.).

**What the provider must offer**

- Issues **JWT access tokens** (signed; the server reads the `at+jwt` or `jwt`
  token type).
- Exposes an **OIDC / OAuth2 discovery document** at its issuer URL
  (`<issuer>/.well-known/openid-configuration`), from which the server fetches the
  signing keys (JWKS).
- Sets a stable **`iss` (issuer)** claim and a **`sub`** claim — `sub` is treated
  as the caller's user id and is what account/expense ownership is scoped to.

**How the server is configured**

Authentication activates by setting a single property to your provider's issuer
URL:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://your-issuer.example.com/
```

The server then validates the signature against the provider's published keys and
checks the standard claims (issuer, expiry). No client secrets or provider
credentials live in this codebase. If the property is left empty, the
issuer-based decoder is not wired up — useful for local/test profiles that
supply their own decoder.

## Configuration & profiles

- `application.properties` — shared defaults
- `application-local.properties` — local DB and RSA key for the `local` profile
- `application-production.properties` — production settings

`./gradlew localRun` starts Postgres via `docker compose up -d --wait`, then
boots the app with the `local` profile.

## Testing strategy

- **Unit / slice tests** (`test`) run on every build.
- **Integration tests** (`integrationTest`) spin up a real PostgreSQL container
  with Testcontainers, and use MockWebServer to stand in for the OAuth provider —
  so auth and persistence are exercised against real-ish dependencies rather than
  mocks.

## Spec-driven development

Behavioral changes are designed before they're coded, using
[OpenSpec](https://github.com/Fission-AI/OpenSpec). Each change starts as a
`proposal.md` + `design.md` + delta `spec.md` under `openspec/changes/`, gets
implemented against its tasks, then is synced into the canonical specs under
`openspec/specs/` and archived. The `openspec/specs/` directory is the
authoritative description of current API behavior.
