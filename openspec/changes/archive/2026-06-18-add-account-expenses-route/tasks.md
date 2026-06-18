## 1. Controller

- [x] 1.1 Inject `ExpenseService` into `AccountController`
- [x] 1.2 Add `GET /accounts/{id}/expenses` endpoint — resolve account via `AccountService.getById(id)`, then delegate to `ExpenseService.listByUser` with the account's `createdBy` as `userId` and the path `id` as `accountId`

## 2. Unit Tests

- [x] 2.1 Add `AccountControllerTest` case: account exists with expenses → 200 with paginated body
- [x] 2.2 Add `AccountControllerTest` case: account not found → 404
- [x] 2.3 Add `AccountControllerTest` case: category + subcategory both provided → 400 (delegated to service)

## 3. Integration Tests

- [x] 3.1 Add `AccountControllerIT` test: existing account with expenses returns 200 with correct items
- [x] 3.2 Add `AccountControllerIT` test: existing account with no expenses returns 200 with empty list
- [x] 3.3 Add `AccountControllerIT` test: unknown account ID returns 404
- [x] 3.4 Add `AccountControllerIT` test: category filter scopes results correctly
- [x] 3.5 Add `AccountControllerIT` test: cursor pagination works across pages
