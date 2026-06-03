## 1. Schema Cleanup

- [x] 1.1 Remove the `CREATE INDEX` statements for `idx_expenses_category_date`, `idx_expenses_subcategory_date`, and `idx_expenses_account_date` from `src/main/resources/db/schema.sql`

## 2. Repository — Criteria Builder

- [x] 2.1 Add `findByFiltersCursor` method to `ExpenseRepository` that dynamically builds the WHERE clause from nullable `category`, `subcategory`, and `accountId` params, applies cursor predicate when present, and orders by `expense_date DESC, expense_id DESC`
- [x] 2.2 Remove `findByCategory`, `countByCategory`, `findBySubcategory`, `countBySubcategory`, `findByAccount`, `countByAccount`, `findByUser` (offset-based), and `countByUser` from `ExpenseRepository`

## 3. Service Layer

- [x] 3.1 Update `ExpenseService.listByUser` to accept nullable `String category`, `String subcategory`, and `Long accountId` parameters
- [x] 3.2 Validate that `category` and `subcategory` are not both present; throw a validation exception (HTTP 400) if so
- [x] 3.3 Pass the three nullable filter params through to `repo.findByFiltersCursor`, replacing the current call to `findByUserCursor`

## 4. Controller Layer

- [x] 4.1 Add optional `category`, `subcategory`, and `accountId` query parameters to the `GET /expenses` handler in `ExpenseController`
- [x] 4.2 Pass the params individually to the service

## 6. Tests — Unit

- [x] 6.1 Update `ExpenseRepositoryTest` — remove tests for deleted methods, add tests for `findByFiltersCursor` covering: no filters, each single filter, and at least one multi-filter combination
- [x] 6.2 Update `ExpenseServiceTest` — remove tests relying on deleted service paths, add tests for `listByUser` with each filter combination
- [x] 6.3 Update `ExpenseControllerTest` — add tests for the new optional query params (present, absent, combined)

## 7. Tests — Integration

- [x] 7.1 Update `ExpenseControllerIT` — add integration test scenarios covering filter by category, subcategory, accountId, and a combination; verify correct results and pagination with filters
