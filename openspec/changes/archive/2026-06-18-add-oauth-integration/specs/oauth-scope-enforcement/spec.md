## ADDED Requirements

### Requirement: Bearer token required on all requests
The system SHALL require a valid JWT Bearer token on every API request. A request without an `Authorization: Bearer <token>` header SHALL be rejected with HTTP 401. A request with a token that is malformed, expired, or signed by an unknown key SHALL be rejected with HTTP 401.

#### Scenario: No Authorization header
- **WHEN** a request is made to any endpoint without an Authorization header
- **THEN** the system returns HTTP 401

#### Scenario: Malformed Bearer token
- **WHEN** a request is made with `Authorization: Bearer not-a-valid-jwt`
- **THEN** the system returns HTTP 401

#### Scenario: Expired JWT
- **WHEN** a request is made with a JWT whose `exp` claim is in the past
- **THEN** the system returns HTTP 401

### Requirement: Scope enforcement for expense read operations
`GET /expenses`, `GET /expenses/{id}`, and `GET /accounts/{id}/expenses` SHALL require the `read:expenses` scope. A valid token that does not carry this scope SHALL be rejected with HTTP 403.

#### Scenario: Token with read:expenses scope is accepted
- **WHEN** `GET /expenses` is called with a token containing `read:expenses`
- **THEN** the system processes the request and returns HTTP 200

#### Scenario: Token missing read:expenses scope is rejected
- **WHEN** `GET /expenses` is called with a valid token that does not contain `read:expenses`
- **THEN** the system returns HTTP 403

#### Scenario: Token missing read:expenses scope is rejected for single-expense lookup
- **WHEN** `GET /expenses/{id}` is called with a valid token that does not contain `read:expenses`
- **THEN** the system returns HTTP 403

#### Scenario: Token missing read:expenses scope is rejected for account-scoped expense listing
- **WHEN** `GET /accounts/{id}/expenses` is called with a valid token that does not contain `read:expenses`
- **THEN** the system returns HTTP 403

### Requirement: Scope enforcement for expense write operations
`POST /expenses`, `POST /expenses/bulk`, `PUT /expenses/{id}`, and `DELETE /expenses/{id}` SHALL require the `write:expenses` scope. A valid token without this scope SHALL be rejected with HTTP 403.

#### Scenario: Token with write:expenses scope is accepted for creation
- **WHEN** `POST /expenses` is called with a token containing `write:expenses`
- **THEN** the system creates the expense and returns HTTP 201

#### Scenario: Token missing write:expenses scope is rejected for deletion
- **WHEN** `DELETE /expenses/{id}` is called with a valid token that does not contain `write:expenses`
- **THEN** the system returns HTTP 403

#### Scenario: Token missing write:expenses scope is rejected for update
- **WHEN** `PUT /expenses/{id}` is called with a valid token that does not contain `write:expenses`
- **THEN** the system returns HTTP 403

### Requirement: Scope enforcement for account read operations
`GET /accounts/{id}` and `GET /accounts/user/{userId}` SHALL require the `read:accounts` scope. A valid token without this scope SHALL be rejected with HTTP 403.

#### Scenario: Token with read:accounts scope is accepted
- **WHEN** `GET /accounts/{id}` is called with a token containing `read:accounts`
- **THEN** the system returns the account and HTTP 200

#### Scenario: Token missing read:accounts scope is rejected
- **WHEN** `GET /accounts/{id}` is called with a valid token that does not contain `read:accounts`
- **THEN** the system returns HTTP 403

### Requirement: Scope enforcement for account write operations
`POST /accounts`, `PUT /accounts/{id}`, and `DELETE /accounts/{id}` SHALL require the `write:accounts` scope. A valid token without this scope SHALL be rejected with HTTP 403.

#### Scenario: Token with write:accounts scope is accepted for creation
- **WHEN** `POST /accounts` is called with a token containing `write:accounts`
- **THEN** the system creates the account and returns HTTP 201

#### Scenario: Token missing write:accounts scope is rejected for deletion
- **WHEN** `DELETE /accounts/{id}` is called with a valid token that does not contain `write:accounts`
- **THEN** the system returns HTTP 403

### Requirement: OAuth issuer URI configured via environment variable
The system SHALL read the OAuth issuer URI from the `OAUTH_ISSUER_URI` environment variable, mapped to `spring.security.oauth2.resourceserver.jwt.issuer-uri`. This is the default mechanism for production environments.

#### Scenario: Production environment sets OAUTH_ISSUER_URI
- **WHEN** the application starts with `OAUTH_ISSUER_URI=https://auth.example.com/`
- **THEN** the JWT validator uses that URI to discover the JWKS endpoint and validate tokens

### Requirement: Local and test environments use a static RSA key
When running under the `local` or `test` Spring profile, the system SHALL validate JWTs using a static RSA public key stored in the classpath, bypassing OIDC discovery. This allows local development and integration tests to run without any external OAuth provider.

#### Scenario: Local profile bypasses issuer discovery
- **WHEN** the application runs with the `local` profile active
- **THEN** JWT validation uses the bundled test RSA public key and does not contact any external JWKS endpoint

#### Scenario: Test profile uses static key for integration tests
- **WHEN** integration tests run with the `test` profile active
- **THEN** JWT validation uses the bundled test RSA public key, enabling tests to mint valid tokens locally
