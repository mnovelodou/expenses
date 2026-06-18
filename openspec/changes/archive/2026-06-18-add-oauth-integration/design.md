## Context

The API has no authentication layer today — any caller can read or mutate any resource. This change adds a Spring Security OAuth 2.0 Resource Server that validates JWT Bearer tokens and enforces scope-based authorization on every endpoint. The JWT issuer URI is supplied as `OAUTH_ISSUER_URI` so the same binary can point at different OAuth providers in each environment. Local development and integration tests must work without a running OAuth provider.

Current stack: Spring Boot 3.5.x, Java 21, JdbcTemplate (no JPA), Testcontainers for integration tests with `WebEnvironment.MOCK` + MockMvc.

## Goals / Non-Goals

**Goals:**
- JWT Bearer token validation via `spring-boot-starter-oauth2-resource-server`
- Scope enforcement per endpoint group (`read:expenses`, `write:expenses`, `read:accounts`, `write:accounts`)
- `OAUTH_ISSUER_URI` env var mapped to `spring.security.oauth2.resourceserver.jwt.issuer-uri`
- Local dev works with a static RSA test key — no external OAuth server required
- Integration tests simulate OAuth using Spring Security Test's `jwt()` post-processor — full filter chain exercised, no server needed
- Integration tests cover 401 (no token) and 403 (wrong scope) scenarios

**Non-Goals:**
- Per-user data isolation (whether a caller can see another user's records)
- Token issuance or introspection endpoints
- Rate limiting per OAuth client
- Refresh token handling

## Decisions

### 1. Spring Security Resource Server over custom filter
Spring Security's OAuth2 Resource Server handles JWKS refresh, clock-skew tolerance, expiry and signature validation, and `SecurityContext` population automatically. A custom servlet filter would replicate all of this. Decision: use `spring-boot-starter-oauth2-resource-server`.

### 2. Scope claim extraction
OAuth providers differ: some put scopes in a `scp` claim (space-delimited string or array), others in `scope`. Spring Security's default converter reads `scope` only. Decision: add a custom `JwtAuthenticationConverter` that checks `scp` first, falls back to `scope`, and emits `GrantedAuthority` with the `SCOPE_` prefix Spring Security expects. Security rules use `hasAuthority("SCOPE_read:expenses")`.

### 3. Local development — static RSA key pair
For local dev, running `bootRun` should not require a live OAuth server. Decision: generate a test RSA key pair; store the public key at `src/main/resources/local/test-rsa-public.pem` and the private key at `src/test/resources/local/test-rsa-private.pem`. The local profile configures `spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:local/test-rsa-public.pem`, bypassing OIDC discovery entirely. A `TestJwtFactory` utility class (in `src/integrationTest`) uses the private key to mint tokens for local curl testing and integration tests alike.

Alternatives considered: embedded WireMock serving a JWKS endpoint, Spring Authorization Server as a test container. Both add runtime complexity; a static key pair is simpler and sufficient for the stated scope.

### 4. Integration test OAuth simulation — `jwt()` RequestPostProcessor
Integration tests use `SpringBootTest.WebEnvironment.MOCK` with MockMvc. Spring Security Test provides `SecurityMockMvcRequestPostProcessors.jwt()`, which injects a pre-authenticated `JwtAuthenticationToken` into the `SecurityContext` before the request hits filters. This exercises the full authorization filter chain (scope checks, 401 on missing header, 403 on missing scope) without performing JWKS validation. Decision: use `jwt()` post-processor; this is Spring's own recommended pattern for resource server testing and keeps tests fast with no network dependencies.

The actual JWKS fetch path is a Spring library concern, not application code — a dedicated smoke/startup test can cover it separately if needed.

### 5. Default integration test token — full-scope
Existing ITs (`ExpenseControllerIT`, `AccountControllerIT`) currently send unauthenticated requests. Post-change they will fail with 401. Decision: add a `jwt(String... scopes)` helper to `BaseIT` and a convenience `fullScopeJwt()` that grants all four scopes. Existing tests call `fullScopeJwt()` so they continue to pass without other changes. New `SecurityIT` tests explicitly use partial-scope or no-auth requests.

## Risks / Trade-offs

- [jwt() post-processor skips actual JWKS validation] → The JWKS fetch code path is not exercised in integration tests. Acceptable trade-off: the JWKS client is a library component. Static-key local validation serves as a partial substitute. A future change can add a `@SpringBootTest(webEnvironment = RANDOM_PORT)` smoke test with WireMock if needed.
- [Test RSA private key committed to source] → Intentional; this key is for non-production use only. The private key lives under `src/test/resources` (excluded from the production JAR by Gradle's default `jar` task configuration). README/comments should document this.
- [Scope format divergence across providers] → Handled by the custom converter checking both `scp` and `scope` claims. If a provider uses a different claim, the converter is the single place to update.

## Migration Plan

1. Add `spring-boot-starter-oauth2-resource-server` to `implementation` in `build.gradle`
2. Generate a 2048-bit RSA key pair; commit public key to `src/main/resources/local/`, private key to `src/test/resources/local/`
3. Configure `application-local.properties`: set `public-key-location` and a placeholder `OAUTH_ISSUER_URI` for reference
4. Configure `application-test.properties`: set `public-key-location` to the test key (integration tests skip OIDC discovery)
5. Add `application.properties` entry: `spring.security.oauth2.resourceserver.jwt.issuer-uri=${OAUTH_ISSUER_URI:}`
6. Create `SecurityConfig` with `SecurityFilterChain` declaring scope rules per endpoint group
7. Create `ScopeClaimConverter` (custom `JwtAuthenticationConverter`)
8. Add `TestJwtFactory` in `src/integrationTest` for minting test tokens
9. Update `BaseIT`: add `jwt(String... scopes)` and `fullScopeJwt()` helpers; apply `fullScopeJwt()` to all existing test helper calls (`createAccount`, `createExpense`, etc.)
10. Write `SecurityIT` covering 401 and 403 scenarios per endpoint group
11. Run `./gradlew integrationTest` — all tests green

**Rollback**: remove `SecurityConfig` bean (or `@Profile`-gate it) to restore the open API.
