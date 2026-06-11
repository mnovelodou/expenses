## 1. Dependencies and Key Material

- [x] 1.1 Add `spring-boot-starter-oauth2-resource-server` to `implementation` in `build.gradle`
- [x] 1.2 Generate a 2048-bit RSA key pair for local/test use (e.g. via `openssl`); store public key at `src/main/resources/local/test-rsa-public.pem` and private key at `src/test/resources/local/test-rsa-private.pem`

## 2. Configuration

- [x] 2.1 Add `spring.security.oauth2.resourceserver.jwt.issuer-uri=${OAUTH_ISSUER_URI:}` to `application.properties`
- [x] 2.2 Add `spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:local/test-rsa-public.pem` to `application-local.properties`
- [x] 2.3 Add `spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:local/test-rsa-public.pem` to `src/integrationTest/resources/application-test.properties`

## 3. Security Configuration

- [x] 3.1 Create `SecurityConfig` class with a `SecurityFilterChain` bean that enables `oauth2ResourceServer(jwt)` and defines scope rules for each endpoint group (`read:expenses`, `write:expenses`, `read:accounts`, `write:accounts`)
- [x] 3.2 Create `ScopeClaimConverter` (implements `Converter<Jwt, AbstractAuthenticationToken>`) that extracts scopes from the `scp` claim (string or list) with fallback to the `scope` claim, and emits `GrantedAuthority` with `SCOPE_` prefix

## 4. Test Infrastructure

- [x] 4.1 Create `TestJwtFactory` in `src/integrationTest/java` that loads `test-rsa-private.pem` and mints signed JWTs with configurable scopes and expiry using `nimbus-jose-jwt` (add `com.nimbusds:nimbus-jose-jwt` to `integrationTestImplementation` in `build.gradle`)
- [x] 4.2 Add `jwt(String... scopes)` helper to `BaseIT` using `SecurityMockMvcRequestPostProcessors.jwt()` that returns a `RequestPostProcessor` with the given scopes as `SCOPE_`-prefixed authorities
- [x] 4.3 Add `fullScopeJwt()` convenience method to `BaseIT` that grants all four scopes (`read:expenses`, `write:expenses`, `read:accounts`, `write:accounts`)
- [x] 4.4 Update all existing `BaseIT` helper methods (`createAccount`, `createExpense`, `createExpenseOnDate`, `createExpenseWithSubcategoryOnDate`) to include `fullScopeJwt()` so existing integration tests continue to pass

## 5. Update Existing Integration Tests

- [x] 5.1 Update `ExpenseControllerIT` — apply `fullScopeJwt()` to all existing `mockMvc.perform(...)` calls that hit expense endpoints
- [x] 5.2 Update `AccountControllerIT` — apply `fullScopeJwt()` to all existing `mockMvc.perform(...)` calls that hit account endpoints

## 6. Security Integration Tests

- [x] 6.1 Create `SecurityIT` class (extends `BaseIT`) with 401 scenarios: assert that `GET /expenses`, `GET /expenses/{id}`, `DELETE /expenses/{id}`, `GET /accounts/{id}`, `DELETE /accounts/{id}`, and `POST /accounts` all return HTTP 401 when called without an Authorization header
- [x] 6.2 Add 403 scenarios to `SecurityIT`: assert HTTP 403 for `GET /expenses` with a `write:expenses`-only token, `DELETE /expenses/{id}` with a `read:expenses`-only token, `GET /accounts/{id}` with a `write:accounts`-only token, and `DELETE /accounts/{id}` with a `read:accounts`-only token
- [x] 6.3 Add positive smoke scenarios to `SecurityIT`: assert that each endpoint group accepts a token with the correct scope (returns something other than 401/403)

## 7. Verification

- [x] 7.1 Run `./gradlew integrationTest` — all tests pass (new security tests green, existing tests unbroken)
- [x] 7.2 Run `./gradlew test` — unit tests unaffected
