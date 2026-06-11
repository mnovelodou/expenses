## ADDED Requirements

### Requirement: Authorization required for expense deletion
`DELETE /expenses/{id}` SHALL reject unauthenticated requests and under-scoped requests before any deletion is attempted. A request without a valid Bearer token SHALL return HTTP 401. A request with a valid token lacking the `write:expenses` scope SHALL return HTTP 403.

#### Scenario: No token — deletion rejected with 401
- **WHEN** `DELETE /expenses/{id}` is called without an Authorization header
- **THEN** the system returns HTTP 401 and no expense is deleted

#### Scenario: Token missing write:expenses — deletion rejected with 403
- **WHEN** `DELETE /expenses/{id}` is called with a valid token that does not contain `write:expenses`
- **THEN** the system returns HTTP 403 and no expense is deleted
