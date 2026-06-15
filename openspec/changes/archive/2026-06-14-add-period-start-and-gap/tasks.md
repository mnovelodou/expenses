## 1. Database Migration

- [x] 1.1 Add SQL migration file: `ALTER TABLE accounts ADD COLUMN period_start DATE;` (nullable, no default)

## 2. Entity & Repository Layer

- [x] 2.1 Add `period_start` (`LocalDate`) field to `AccountEntity`
- [x] 2.2 Update `AccountRepository` SQL queries (insert, update, select) to include `period_start`
- [x] 2.3 Add gap query method to `AccountRepository`: aggregate `SUM(expenses.amount)` by `account_id` where `expense_date >= period_start`

## 3. DTOs

- [x] 3.1 Add `period_start` (`LocalDate`) to `Account` response DTO
- [x] 3.2 Add `gap` (`BigDecimal`, nullable) to `Account` response DTO
- [x] 3.3 Add `period_start` (required) to `CreateAccountRequest` DTO with validation
- [x] 3.4 Add `period_start` (optional) to `UpdateAccountRequest` DTO (null = keep existing)

## 4. Service Layer

- [x] 4.1 Update `AccountService` create logic to pass `period_start` through to repository
- [x] 4.2 Update `AccountService` update logic: only overwrite `period_start` when the incoming value is non-null
- [x] 4.3 Update `AccountService` read logic: compute and attach `gap` when `includeGap=true`, skip otherwise

## 5. Controller Layer

- [x] 5.1 Add `includeGap` boolean query param to `GET /accounts/{id}` in `AccountController`
- [x] 5.2 Add `includeGap` boolean query param to `GET /accounts` in `AccountController`
- [x] 5.3 Pass `includeGap` flag down to `AccountService`

## 6. Mapper

- [x] 6.1 Update `AccountMapper` to map `period_start` and `gap` fields between entity, DTO, and response

## 7. Tests

- [x] 7.1 Update `AccountRepositoryTest` to cover `period_start` in insert/select
- [x] 7.2 Add repository test for gap query (with and without matching expenses)
- [x] 7.3 Update `AccountServiceTest` for create/update/read with `period_start`
- [x] 7.4 Add service test: gap included when `includeGap=true`, absent otherwise, null when `period_start` is null
- [x] 7.5 Update `AccountControllerTest` and integration tests for new request/response shapes
