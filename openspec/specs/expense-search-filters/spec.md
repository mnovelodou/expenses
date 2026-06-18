# expense-search-filters

## Purpose

Optional filtering parameters for the `GET /expenses` endpoint, allowing results to be narrowed by category, subcategory, and account.
## Requirements
### Requirement: Authorization enforced before filter evaluation
`GET /expenses` with optional filters SHALL apply authorization checks before any filter or pagination logic is executed. A request without a valid Bearer token SHALL return HTTP 401. A request with a valid token lacking the `read:expenses` scope SHALL return HTTP 403. Filter parameter validation (e.g., mutual exclusion of `category` and `subcategory`) is not reached for unauthorized requests.

#### Scenario: No token with filter params — rejected with 401 before filter validation
- **WHEN** `GET /expenses?category=Food&subcategory=Groceries` is called without an Authorization header
- **THEN** the system returns HTTP 401 without evaluating the conflicting filter parameters

#### Scenario: Token missing read:expenses with valid filters — rejected with 403
- **WHEN** `GET /expenses?category=Food` is called with a valid token that does not contain `read:expenses`
- **THEN** the system returns HTTP 403 without executing any expense query logic

### Requirement: Optional filters on cursor-paginated expense list
The `GET /expenses` endpoint SHALL accept three optional query parameters: `category` (string), `subcategory` (string), and `accountId` (integer). Since a subcategory belongs to exactly one category, `category` and `subcategory` are mutually exclusive — supplying both SHALL be rejected with HTTP 400. `accountId` MAY be combined with either `category` or `subcategory`. When a filter is absent, it SHALL NOT constrain the results. The date range remains mandatory as defined in the `list-expenses-by-user` spec; `user_id` is optional and defaults to the authenticated caller.

#### Scenario: No optional filters — all expenses returned
- **WHEN** a GET request is made with only `user_id` and date range
- **THEN** the system returns HTTP 200 with all expenses for that user and date window

#### Scenario: Filter by category only
- **WHEN** a GET request includes `category=Food`
- **THEN** the system returns only expenses where `category = 'Food'` for that user and date window

#### Scenario: Filter by subcategory only
- **WHEN** a GET request includes `subcategory=Groceries`
- **THEN** the system returns only expenses where `subcategory = 'Groceries'` for that user and date window

#### Scenario: Filter by accountId only
- **WHEN** a GET request includes `accountId=3`
- **THEN** the system returns only expenses where `account_id = 3` for that user and date window

#### Scenario: Filter by subcategory and accountId combined
- **WHEN** a GET request includes both `subcategory=Groceries` and `accountId=3`
- **THEN** the system returns only expenses matching both predicates

#### Scenario: Filter by category and accountId combined
- **WHEN** a GET request includes both `category=Food` and `accountId=3`
- **THEN** the system returns only expenses matching both predicates

#### Scenario: category and subcategory supplied together — rejected
- **WHEN** a GET request includes both `category=Food` and `subcategory=Groceries`
- **THEN** the system returns HTTP 400

### Requirement: Cursor pagination works with filters
The cursor-paginated response SHALL work correctly when optional filters are present. The cursor encodes only `(expense_date, expense_id)` and does NOT encode filter state. The caller SHALL supply the same filter parameters on every page request.

#### Scenario: Subsequent page with filters preserved
- **WHEN** a follow-up request includes a valid `cursor` and the same filter parameters as the first page
- **THEN** the system returns the next page of filtered results in the correct order

#### Scenario: nextCursor is null on last filtered page
- **WHEN** the current page contains the last expense matching the active filters
- **THEN** the response contains `nextCursor: null`

