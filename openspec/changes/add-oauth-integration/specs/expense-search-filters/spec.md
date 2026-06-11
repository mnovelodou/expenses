## ADDED Requirements

### Requirement: Authorization enforced before filter evaluation
`GET /expenses` with optional filters SHALL apply authorization checks before any filter or pagination logic is executed. A request without a valid Bearer token SHALL return HTTP 401. A request with a valid token lacking the `read:expenses` scope SHALL return HTTP 403. Filter parameter validation (e.g., mutual exclusion of `category` and `subcategory`) is not reached for unauthorized requests.

#### Scenario: No token with filter params — rejected with 401 before filter validation
- **WHEN** `GET /expenses?category=Food&subcategory=Groceries` is called without an Authorization header
- **THEN** the system returns HTTP 401 without evaluating the conflicting filter parameters

#### Scenario: Token missing read:expenses with valid filters — rejected with 403
- **WHEN** `GET /expenses?category=Food` is called with a valid token that does not contain `read:expenses`
- **THEN** the system returns HTTP 403 without executing any expense query logic
