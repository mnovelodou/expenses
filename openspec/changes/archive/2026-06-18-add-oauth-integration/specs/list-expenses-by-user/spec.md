## ADDED Requirements

### Requirement: Authorization required for expense listing
`GET /expenses` SHALL reject unauthenticated requests and under-scoped requests before any other validation is applied. A request without a valid Bearer token SHALL return HTTP 401. A request with a valid token lacking the `read:expenses` scope SHALL return HTTP 403.

#### Scenario: No token — listing rejected with 401
- **WHEN** `GET /expenses` is called without an Authorization header
- **THEN** the system returns HTTP 401 without executing any expense query logic

#### Scenario: Token missing read:expenses — listing rejected with 403
- **WHEN** `GET /expenses` is called with a valid token that does not contain `read:expenses`
- **THEN** the system returns HTTP 403 without executing any expense query logic
