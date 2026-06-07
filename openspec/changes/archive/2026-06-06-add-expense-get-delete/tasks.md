## 1. Exception Handling

- [x] 1.1 Add `ExpenseNotFoundException` (or reuse existing pattern in `ExpenseServiceExceptions`) mapped to HTTP 404 in `GlobalExceptionHandler`

## 2. Service Layer

- [x] 2.1 Add `getById(Long id)` to `ExpenseService` — call `repository.get(id)`, throw not-found exception if empty, map to `Expense` DTO
- [x] 2.2 Add `delete(Long id)` to `ExpenseService` — call `repository.delete(id)`, throw not-found exception if it returns `false`

## 3. Controller Layer

- [x] 3.1 Add `GET /expenses/{id}` endpoint to `ExpenseController` returning `Expense` (200)
- [x] 3.2 Add `DELETE /expenses/{id}` endpoint to `ExpenseController` returning 204 No Content

## 4. Unit Tests

- [x] 4.1 Add `ExpenseServiceTest` cases for `getById`: found and not-found
- [x] 4.2 Add `ExpenseServiceTest` cases for `delete`: success and not-found
- [x] 4.3 Add `ExpenseControllerTest` cases for `GET /expenses/{id}`: 200 and 404
- [x] 4.4 Add `ExpenseControllerTest` cases for `DELETE /expenses/{id}`: 204 and 404

## 5. Integration Tests

- [x] 5.1 Add `ExpenseControllerIT` test for `GET /expenses/{id}` — existing expense returns 200 with correct body
- [x] 5.2 Add `ExpenseControllerIT` test for `GET /expenses/{id}` — unknown ID returns 404
- [x] 5.3 Add `ExpenseControllerIT` test for `DELETE /expenses/{id}` — existing expense returns 204 and is no longer retrievable
- [x] 5.4 Add `ExpenseControllerIT` test for `DELETE /expenses/{id}` — unknown ID returns 404
