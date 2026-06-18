## Why

The expenses API has no way to list expenses yet. Adding a paginated list endpoint is the first step toward a usable read API, and cursor-based pagination is chosen over offset pagination to keep query performance predictable as expense history grows.

## What Changes

- Add `GET /expenses` endpoint scoped to the authenticated user
- Required query param: `user_id` (temporary until OAuth authentication is in place, at which point it will be inferred from the token)
- Optional query params: `start_date`, `end_date` (independently defaulted when absent; max 3-calendar-month range); `cursor` and `limit` also optional
- Results returned in descending chronological order (`expense_date DESC`, `expense_id DESC`)
- Cursor is an opaque base64 token encoding the last item's position (`expense_date` + `expense_id`); forward-only for now
- New `CursorPageResponse<T>` DTO replacing offset-based `PageResponse` for this endpoint
- Index on `expenses(created_by, expense_date DESC, expense_id DESC)` replaces `(created_by, expense_date)` to cover the tie-breaker column

## Capabilities

### New Capabilities

- `list-expenses-by-user`: Paginated listing of expenses for a user within a date window (optional, independently defaulted, capped at 3 calendar months), using forward cursor pagination

### Modified Capabilities

## Impact

- `ExpenseController`: new GET handler
- `ExpenseService`: new query method
- `ExpenseRepository`: new `findByUserCursor` method with optional cursor condition
- `schema.sql`: updated index definition
- New DTO: `CursorPageResponse<T>`
- No breaking changes to existing create/update endpoints
