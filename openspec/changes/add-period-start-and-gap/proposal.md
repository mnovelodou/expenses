## Why

Users track monthly spending by copying their bank balance into the app and logging expenses until the unexplained gap reaches zero. There is currently no way for the API to represent the start of a tracking period or to compute that gap — users must do it manually outside the app.

## What Changes

- Add `period_start` (DATE, nullable) column to the `accounts` table via a migration
- `POST /accounts` — `period_start` required
- `PATCH /accounts/{id}` — `period_start` optional; omitting or sending null keeps the existing value
- `GET /accounts` and `GET /accounts/{id}` — return `period_start` in the response
- Add opt-in gap calculation via `?includeGap=true` query param on GET endpoints
  - Gap = `current_amount - initial_amount - SUM(expenses.amount WHERE expense_date >= period_start)`
  - Returns `null` when `period_start` is null
  - Field excluded entirely from response when flag is not set (no DB query)

## Capabilities

### New Capabilities

- `account-period-start`: Introduces the `period_start` field on accounts — required at creation, optional on update, returned on reads
- `account-gap`: Opt-in gap calculation on account GET endpoints using expenses logged since `period_start`

### Modified Capabilities

<!-- No existing specs have requirement-level changes -->

## Impact

- **Database**: Schema migration adds `period_start DATE` (nullable) to `accounts`
- **API**: Account create/update/read endpoints updated
- **Existing data**: Unaffected — existing rows get `period_start = NULL`
- **Expenses**: Read (not modified) to compute gap; `account_id` FK already exists
