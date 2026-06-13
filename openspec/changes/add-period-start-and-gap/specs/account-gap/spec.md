## ADDED Requirements

### Requirement: Gap is opt-in via query parameter
The system SHALL compute and return the account gap only when the request includes `?includeGap=true`. When the parameter is absent or false, the `gap` field SHALL be excluded entirely from the response and no expense aggregation query SHALL be performed.

#### Scenario: Request without includeGap
- **WHEN** a GET /accounts/{id} request is made without `?includeGap=true`
- **THEN** the response does not include a `gap` field

#### Scenario: Request with includeGap=true
- **WHEN** a GET /accounts/{id}?includeGap=true request is made
- **THEN** the response includes a `gap` field

### Requirement: Gap is calculated from expenses since period_start
When `includeGap=true`, the system SHALL compute gap as:
`current_amount - initial_amount - SUM(expenses.amount WHERE expense_date >= period_start AND account_id = <id>)`

The `expense_date >= period_start` boundary is inclusive — expenses on the start date itself are included.

#### Scenario: Gap with logged expenses
- **WHEN** a GET /accounts/{id}?includeGap=true is requested and the account has `period_start` set and expenses with `expense_date >= period_start`
- **THEN** the `gap` equals `current_amount - initial_amount - SUM(matching expenses)`

#### Scenario: Gap with no expenses since period_start
- **WHEN** a GET /accounts/{id}?includeGap=true is requested and no expenses exist with `expense_date >= period_start`
- **THEN** the `gap` equals `current_amount - initial_amount`

### Requirement: Gap is null when period_start is not set
When `includeGap=true` but `period_start` is null, the system SHALL return `"gap": null`.

#### Scenario: Gap requested but period_start is null
- **WHEN** a GET /accounts/{id}?includeGap=true is requested for an account with no `period_start`
- **THEN** the response includes `"gap": null`

### Requirement: includeGap is supported on list endpoint
The system SHALL support `?includeGap=true` on GET /accounts in addition to GET /accounts/{id}, computing the gap for each account in the result set.

#### Scenario: List accounts with includeGap
- **WHEN** a GET /accounts?includeGap=true request is made
- **THEN** each account in the response includes a `gap` field (value or null per account)
