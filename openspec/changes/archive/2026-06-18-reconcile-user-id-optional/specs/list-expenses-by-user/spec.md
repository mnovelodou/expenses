## MODIFIED Requirements

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
