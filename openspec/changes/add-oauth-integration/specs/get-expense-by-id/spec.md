## ADDED Requirements

### Requirement: Authorization required for single expense retrieval
`GET /expenses/{id}` SHALL reject unauthenticated requests and under-scoped requests before looking up the expense. A request without a valid Bearer token SHALL return HTTP 401. A request with a valid token lacking the `read:expenses` scope SHALL return HTTP 403.

#### Scenario: No token — retrieval rejected with 401
- **WHEN** `GET /expenses/{id}` is called without an Authorization header
- **THEN** the system returns HTTP 401

#### Scenario: Token missing read:expenses — retrieval rejected with 403
- **WHEN** `GET /expenses/{id}` is called with a valid token that does not contain `read:expenses`
- **THEN** the system returns HTTP 403
