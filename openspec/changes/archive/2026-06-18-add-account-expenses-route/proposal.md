## Why

`GET /expenses?account_id={id}` works but requires callers to know the filter parameter name and always pass a user_id. A nested route `GET /accounts/{id}/expenses` is a more natural REST pattern for the common case of "show me all expenses on this account."

## What Changes

- Add `GET /accounts/{id}/expenses` — lists expenses scoped to a specific account, with the same pagination, date-window, and category/subcategory filter support as the existing list endpoint

## Capabilities

### New Capabilities
- `list-expenses-by-account`: Paginated expense list scoped to an account ID, supporting cursor pagination, date window, and category/subcategory filters

### Modified Capabilities
<!-- none -->

## Impact

- `AccountController` — one new endpoint method
- `ExpenseService` — reuses existing `listByUser` logic; `account_id` is mandatory, `user_id` is inferred from the account
- `AccountService.getById` — called to resolve the account owner (validates account exists and provides the `createdBy` user)
- No schema changes
