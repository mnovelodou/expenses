## Why

The current expense list endpoint filters only by user and date range. Users need to narrow results by category, subcategory, or account — common queries in any expense tracking workflow. Adding these as optional filters on the existing cursor-paginated endpoint avoids the proliferation of separate per-filter methods that already exists in the codebase and cleans it up.

## What Changes

- Add optional query parameters `category`, `subcategory`, and `account_id` to the cursor-paginated `GET /expenses` endpoint
- `userId` and date range remain mandatory
- `category` and `subcategory` are mutually exclusive — since a subcategory belongs to exactly one category, supplying both is rejected with HTTP 400; `account_id` may be combined with either
- Replace the per-filter repository methods (`findByCategory`, `findBySubcategory`, `findByAccount`, `findByUser` offset-based, and their `countBy*` counterparts) with a single criteria builder method
- Introduce a `CategoryFilter` sealed union type to represent the `category | subcategory` choice, making the mutual exclusivity explicit in the type system
- Remove the three now-redundant indexes (`idx_expenses_category_date`, `idx_expenses_subcategory_date`, `idx_expenses_account_date`) from `schema.sql`; the existing `idx_expenses_user_date` index is sufficient given the mandatory date range constraint

## Capabilities

### New Capabilities
- `expense-search-filters`: Optional category, subcategory, and accountId filters on the cursor-paginated expense list, backed by a dynamic criteria builder in the repository layer

### Modified Capabilities
- `list-expenses-by-user`: The existing list capability is extended with optional filter parameters; the underlying query method and dead per-filter methods are replaced

## Impact

- `ExpenseRepository`: remove 8 methods, add 1 criteria builder cursor method
- `ExpenseService`: update `listByUser` to accept and pass through optional filters
- `ExpenseController`: add optional `category`, `subcategory`, `account_id` query params; build `CategoryFilter` union from the two mutually exclusive params
- `schema.sql`: remove 3 `CREATE INDEX` statements
- Existing integration and unit tests for the removed methods will be deleted or updated
