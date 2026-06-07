## 1. DTOs

- [x] 1.1 Create `BulkCreateExpensesRequest` DTO with a `List<CreateExpenseRequest> expenses` field and a `@Size(min=1, max=200)` constraint
- [x] 1.2 Create `BulkCreateExpensesResponse` DTO with a `List<CreateExpenseResponse> expenses` field

## 2. Repository

- [x] 2.1 Add `bulkInsert(List<ExpenseEntity> expenses): List<ExpenseEntity>` to `ExpenseRepository` using `JdbcTemplate.batchUpdate` and return the inserted entities with generated IDs

## 3. Service

- [x] 3.1 Add `bulkCreate(List<CreateExpenseRequest> requests): List<CreateExpenseResponse>` to `ExpenseService` — validate each item (reuse existing validation / account existence check), call `repository.bulkInsert`, and wrap the whole operation in `@Transactional`

## 4. Controller

- [x] 4.1 Add `POST /expenses/bulk` handler to `ExpenseController` that accepts `@Valid BulkCreateExpensesRequest`, delegates to `ExpenseService.bulkCreate`, and returns HTTP 201 with `BulkCreateExpensesResponse`

## 5. OpenAPI Spec

- [x] 5.1 Document the `POST /expenses/bulk` endpoint in the OpenAPI spec (`openapi.yaml` or equivalent) with request/response schemas and error responses (400, 500)

## 6. Tests

- [x] 6.1 Add unit tests in `ExpenseServiceTest` for `bulkCreate`: success, empty list, item with invalid accountId, list exceeding 200 items
- [x] 6.2 Add unit tests in `ExpenseControllerTest` for the new endpoint: 201 success, 400 on validation failure
- [x] 6.3 Add integration tests in `ExpenseControllerIT` covering: successful bulk creation (verify all rows in DB), one invalid item rolls back all inserts, batch size exceeded
