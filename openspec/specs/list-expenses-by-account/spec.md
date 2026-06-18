# list-expenses-by-account Specification

## Purpose
TBD - created by archiving change add-account-expenses-route. Update Purpose after archive.
## Requirements
### Requirement: List expenses by account
The system SHALL expose `GET /accounts/{id}/expenses` that returns a cursor-paginated list of expenses belonging to the given account, supporting the same date-window, limit, cursor, category, and subcategory filters as `GET /expenses`.

#### Scenario: Account exists with expenses
- **WHEN** a caller sends `GET /accounts/{id}/expenses` for an account that has expenses in the requested date window
- **THEN** the system returns HTTP 200 with a paginated list of expenses for that account

#### Scenario: Account exists with no expenses
- **WHEN** a caller sends `GET /accounts/{id}/expenses` for an account that has no expenses in the requested date window
- **THEN** the system returns HTTP 200 with an empty content list

#### Scenario: Account not found
- **WHEN** a caller sends `GET /accounts/{id}/expenses` with an account ID that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: category and subcategory both provided
- **WHEN** a caller sends `GET /accounts/{id}/expenses` with both `category` and `subcategory` query params
- **THEN** the system returns HTTP 400

#### Scenario: Cursor pagination works
- **WHEN** a caller sends `GET /accounts/{id}/expenses` with a `cursor` from a previous response
- **THEN** the system returns the next page of results relative to that cursor

#### Scenario: Date window defaults apply
- **WHEN** no `start_date` or `end_date` are provided
- **THEN** the system defaults to the previous calendar month, consistent with `GET /expenses`

