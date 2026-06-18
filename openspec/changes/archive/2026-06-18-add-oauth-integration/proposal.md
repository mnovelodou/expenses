## Why

The API currently has no authentication or authorization — any caller can read or mutate any data. Adding OAuth 2.0 JWT-based scope enforcement closes this gap and establishes the foundation for per-user data access control in future changes.

## What Changes

- Add Spring Security Resource Server dependency and configuration
- Require a valid Bearer JWT on every request; return **401 Unauthorized** when missing or invalid
- Enforce scope-based authorization per endpoint group; return **403 Forbidden** when the token lacks the required scope:
  - `read:expenses` — `GET /expenses`, `GET /expenses/{id}`, `GET /accounts/{id}/expenses`
  - `write:expenses` — `POST /expenses`, `POST /expenses/bulk`, `PUT /expenses/{id}`, `DELETE /expenses/{id}`
  - `read:accounts` — `GET /accounts/{id}`, `GET /accounts/user/{userId}`
  - `write:accounts` — `POST /accounts`, `PUT /accounts/{id}`, `DELETE /accounts/{id}`
- Configure the OAuth issuer URI via the environment variable `OAUTH_ISSUER_URI`
- Provide a WireMock-based fake OAuth server for local development and integration tests so no real IdP is required
- Add integration tests that assert 401 and 403 responses for unauthenticated and under-scoped requests

**Not in scope:** user-level data isolation (whether a caller can access another user's records) is deferred to a future change.

## Capabilities

### New Capabilities
- `oauth-scope-enforcement`: JWT Bearer token validation and scope-based access control across all endpoints, including fake-server setup for local/test environments

### Modified Capabilities
- `list-expenses-by-user`: now returns 401 when unauthenticated, 403 when token lacks `read:expenses`
- `get-expense-by-id`: now returns 401 when unauthenticated, 403 when token lacks `read:expenses`
- `delete-expense`: now returns 401 when unauthenticated, 403 when token lacks `write:expenses`
- `expense-search-filters`: filters still behave the same; requests without a valid `read:expenses` token now return 401/403 before filters are evaluated

## Impact

- **Dependencies**: `spring-boot-starter-oauth2-resource-server` added to main; `wiremock-standalone` (or Spring Boot WireMock) added to `integrationTestImplementation`
- **Configuration**: new property `spring.security.oauth2.resourceserver.jwt.issuer-uri` mapped from `OAUTH_ISSUER_URI`; local profile gets a static WireMock JWKS URL; test profile gets a dynamically bound WireMock port
- **Integration tests**: `BaseIT` gains fake-JWT helpers; all existing IT classes remain green because they will receive a valid full-scope token by default; new `SecurityIT` class covers the 401/403 scenarios
- **Controllers**: no changes to controller logic — security enforced entirely via Spring Security filter chain
