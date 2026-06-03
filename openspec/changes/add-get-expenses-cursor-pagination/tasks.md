## 1. Schema & Index

- [x] 1.1 Update `schema.sql`: replace `idx_expenses_user_date ON expenses(created_by, expense_date)` with `(created_by, expense_date DESC, expense_id DESC)`

## 2. DTOs

- [x] 2.1 Create `CursorPageResponse<T>` record with fields: `content` (List<T>), `nextCursor` (String, nullable), `pageSize` (int)

## 3. Cursor Encoding

- [x] 3.1 Create `ExpenseCursor` utility class with `encode(expense_date, expense_id): String` and `decode(token): (LocalDate, Long)` using base64 JSON `{"d":"...","id":...}`
- [x] 3.2 Add validation: `decode` throws `InvalidCursorException` (maps to HTTP 400) on malformed input; validate separately that the decoded date falls within the request's date range

## 4. Repository

- [x] 4.1 Add `findByUserCursor(userId, startDate, endDate, limit, cursor?)` method to `ExpenseRepository` — orders results descending by date then id, and adds a positional predicate when a cursor is present to fetch only expenses older than the cursor position

## 5. Service

- [x] 5.1 Add `listByUser(userId, startDate, endDate, limit, cursor?)` method to `ExpenseService`
- [x] 5.2 Apply date defaults using calendar-aware date arithmetic: both absent → previous calendar month; only `start_date` → `end_date = start_date + 1 month`; only `end_date` → `start_date = end_date - 1 month`; reject with 400 if `end_date < start_date` or range exceeds 3 calendar months
- [x] 5.3 Validate `limit`: reject with 400 if zero, negative, or above the maximum of 100; apply a default of 20 if absent
- [x] 5.4 Call repository, map results to `Expense` DTOs, encode `nextCursor` from last item if result size == limit, return `CursorPageResponse`

## 6. Controller

- [x] 6.1 Add `GET /expenses` handler to `ExpenseController` accepting required query parameter `user_id` and optional query parameters `start_date`, `end_date`, `limit`, `cursor`
- [x] 6.2 Validate that `user_id` is present and non-blank — return 400 if missing

## 7. Exception Handling

- [x] 7.1 Register `InvalidCursorException` in `GlobalExceptionHandler` → HTTP 400
- [x] 7.2 Register date validation errors → HTTP 400 with descriptive message

## 8. Tests

- [x] 8.1 Unit test `ExpenseCursor`: encode/decode round-trip, malformed input throws
- [x] 8.2 Unit test `ExpenseService.listByUser`: both dates absent → last month default, only `start_date` → `end_date` derived, only `end_date` → `start_date` derived, `end_date < start_date` → 400, range > 3 calendar months → 400, `limit` above cap → 400, `limit` zero/negative → 400, valid request returns correct `CursorPageResponse`
- [x] 8.3 Repository test: first page (no cursor), subsequent page (with cursor), last page (`nextCursor` is null)
- [x] 8.4 Integration test `GET /expenses`: happy path first page, pagination with cursor, malformed cursor → 400, cursor date outside range → 400, both dates absent → defaults to last month, range too large → 400, `end_date < start_date` → 400, `limit` above cap → 400, missing `user_id` → 400
