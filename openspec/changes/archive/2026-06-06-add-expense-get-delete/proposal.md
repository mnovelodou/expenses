## Why

The expenses API currently supports creating and updating expenses but has no way to fetch a single expense by ID or delete one. This leaves the CRUD surface incomplete — callers must page through list results to retrieve a known expense and have no recourse for removing erroneous entries.

## What Changes

- Add `GET /expenses/{id}` — returns a single expense by its ID
- Add `DELETE /expenses/{id}` — removes an expense by its ID, returning 204 No Content

## Capabilities

### New Capabilities
- `get-expense-by-id`: Fetch a single expense by its numeric ID
- `delete-expense`: Delete an expense by its numeric ID

### Modified Capabilities
<!-- none -->

## Impact

- `ExpenseController` — two new endpoint methods
- `ExpenseService` — two new service methods
- `ExpenseRepository` — uses existing JPA `findById` / `deleteById`
- New exception type for expense-not-found (404)
- Integration tests for both endpoints
