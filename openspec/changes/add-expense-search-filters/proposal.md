## Why

The current expense list endpoint filters only by user and date range. Users need to narrow results by category, subcategory, or account — common queries in any expense tracking workflow. Adding these as optional filters on the existing cursor-paginated endpoint avoids the proliferation of separate per-filter methods that already exists in the codebase and cleans it up.

## What Changes

- Add optional query parameters `category`, `subcategory`, and `accountId` to the cursor-paginated `GET /expenses` endpoint
- `userId` and date range remain mandatory
- Replace the per-filter repository methods (`findByCategory`, `findBySubcategory`, `findByAccount`, `findByUser` offset-based, and their `countBy*` counterparts) with a single criteria builder method
- Remove the three now-redundant indexes (`idx_expenses_category_date`, `idx_expenses_subcategory_date`, `idx_expenses_account_date`) from `schema.sql`; the existing `idx_expenses_user_date` index is sufficient given the mandatory date range constraint
- Any combination of the optional filters is supported (e.g. category + accountId, all three, none)

## Capabilities

### New Capabilities
- `expense-search-filters`: Optional category, subcategory, and accountId filters on the cursor-paginated expense list, backed by a dynamic criteria builder in the repository layer

### Modified Capabilities
- `list-expenses-by-user`: The existing list capability is extended with optional filter parameters; the underlying query method and dead per-filter methods are replaced

## Impact

- `ExpenseRepository`: remove 8 methods, add 1 criteria builder cursor method
- `ExpenseService`: update `listByUser` to accept and pass through optional filters
- `ExpenseController`: add optional `category`, `subcategory`, `accountId` query params
- `schema.sql`: remove 3 `CREATE INDEX` statements
- Existing integration and unit tests for the removed methods will be deleted or updated
