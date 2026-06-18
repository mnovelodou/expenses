# list-expenses-by-user

## Purpose

Paginated listing of expenses for a given user within a date window, using forward cursor pagination.
## Requirements
### Requirement: Authorization required for expense listing
`GET /expenses` SHALL reject unauthenticated requests and under-scoped requests before any other validation is applied. A request without a valid Bearer token SHALL return HTTP 401. A request with a valid token lacking the `read:expenses` scope SHALL return HTTP 403.

#### Scenario: No token — listing rejected with 401
- **WHEN** `GET /expenses` is called without an Authorization header
- **THEN** the system returns HTTP 401 without executing any expense query logic

#### Scenario: Token missing read:expenses — listing rejected with 403
- **WHEN** `GET /expenses` is called with a valid token that does not contain `read:expenses`
- **THEN** the system returns HTTP 403 without executing any expense query logic

### Requirement: List expenses by user with date window
The system SHALL expose a `GET /expenses` endpoint that returns a paginated list of expenses for a given user. `user_id` is an OPTIONAL query parameter that defaults to the authenticated caller's `sub`; when supplied it MUST equal the caller's `sub`, and a non-matching value SHALL be hidden as HTTP 404 (see the `owner-scoped-authorization` spec). `start_date` and `end_date` are both optional and independently defaulted:
- Both absent: default to the previous calendar month (first day to last day).
- Only `start_date` provided: `end_date` defaults to `start_date` plus 1 month.
- Only `end_date` provided: `start_date` defaults to `end_date` minus 1 month.

The date range from `start_date` to `end_date` SHALL NOT exceed 3 calendar months; requests exceeding this limit SHALL be rejected with HTTP 400. Optional filters `category`, `subcategory`, `accountId`, and `transaction_amount` MAY be supplied to narrow results; see the `expense-search-filters` and `transaction-amount` specs.

#### Scenario: Missing user_id — defaults to the caller
- **WHEN** a GET request is made to `/expenses` without a `user_id` parameter
- **THEN** the system returns HTTP 200 with the authenticated caller's own expenses for the resolved date window

#### Scenario: Successful first-page request with explicit date range
- **WHEN** a GET request is made to `/expenses?user_id=u1&start_date=2026-03-01&end_date=2026-05-31&limit=20` by caller `u1`
- **THEN** the system returns HTTP 200 with the first page of expenses in descending chronological order

#### Scenario: Both date parameters absent — defaults to last calendar month
- **WHEN** a GET request is made to `/expenses` without `start_date` or `end_date`
- **THEN** the system returns HTTP 200 with expenses from the first to the last day of the previous calendar month

#### Scenario: Only start_date provided — end_date defaults to start_date plus one month
- **WHEN** a GET request is made with `start_date=2026-04-01` and no `end_date`
- **THEN** the system returns HTTP 200 with expenses from `2026-04-01` to `2026-05-01`

#### Scenario: Only end_date provided — start_date defaults to end_date minus one month
- **WHEN** a GET request is made with `end_date=2026-05-01` and no `start_date`
- **THEN** the system returns HTTP 200 with expenses from `2026-04-01` to `2026-05-01`

#### Scenario: Date range exceeds 3 calendar months
- **WHEN** a GET request is made with a date range spanning more than 3 calendar months
- **THEN** the system returns HTTP 400

#### Scenario: end_date is before start_date
- **WHEN** a GET request is made with `end_date` earlier than `start_date`
- **THEN** the system returns HTTP 400

#### Scenario: Filter by transaction_amount within the date window
- **WHEN** a GET request includes `transaction_amount=100.00` alongside the optional `user_id` (defaulting to the caller) and date range
- **THEN** the system returns HTTP 200 with only expenses whose `transaction_amount = 100.00` in the window

### Requirement: Forward cursor pagination
The system SHALL support forward-only cursor pagination via an optional `cursor` query parameter. On the first page the parameter SHALL be omitted. The response SHALL include a `nextCursor` field that is a non-null opaque string when more results exist, and null when the current page is the last page within the date window.

#### Scenario: First page has more results
- **WHEN** total matching expenses exceed the requested `limit`
- **THEN** the response contains a non-null `nextCursor`

#### Scenario: Last page of results
- **WHEN** the current page contains the last matching expense in the date window
- **THEN** the response contains `nextCursor: null`

#### Scenario: Subsequent page using cursor
- **WHEN** a GET request includes a valid `cursor` value from a previous response
- **THEN** the system returns the next page of expenses older than the cursor position, within the same date window

#### Scenario: Malformed cursor value
- **WHEN** a GET request includes a cursor that cannot be decoded
- **THEN** the system returns HTTP 400

#### Scenario: Cursor date outside the request date range
- **WHEN** a GET request includes a cursor whose encoded date falls outside the provided `start_date`/`end_date` window
- **THEN** the system returns HTTP 400

### Requirement: Descending chronological order
Results SHALL be ordered by `expense_date DESC`, with `expense_id DESC` as a stable tie-breaker when multiple expenses share the same date.

#### Scenario: Multiple expenses on the same date
- **WHEN** several expenses exist on the same `expense_date`
- **THEN** they are returned in descending `expense_id` order within that date

### Requirement: Page size limit
The `limit` query parameter is optional. When absent the system SHALL apply a default page size. The system SHALL enforce a maximum page size; requests exceeding the maximum SHALL be rejected with HTTP 400. Zero or negative values for `limit` SHALL be rejected with HTTP 400.

#### Scenario: limit absent — default page size applied
- **WHEN** a GET request is made without a `limit` parameter
- **THEN** the system returns HTTP 200 with a page using the default page size

#### Scenario: limit exceeds maximum — rejected
- **WHEN** a GET request is made with `limit` greater than the server maximum
- **THEN** the system returns HTTP 400

#### Scenario: limit is zero or negative — rejected
- **WHEN** a GET request is made with `limit=0` or a negative `limit`
- **THEN** the system returns HTTP 400

### Requirement: Cursor page response shape
The endpoint SHALL return a `CursorPageResponse` with `content` (list of expenses), `nextCursor` (string or null), and `pageSize` (int). The response SHALL NOT include total element count or total page count.

#### Scenario: Response structure matches contract
- **WHEN** a valid request is made to `GET /expenses`
- **THEN** the response body contains `content`, `nextCursor`, and `pageSize` fields

