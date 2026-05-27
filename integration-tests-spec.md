# Integration Tests Spec

## What problem this solves

The existing tests are all unit tests: `@WebMvcTest` stubs the service layer, repository tests mock JDBC. No test currently exercises the full HTTP → controller → service → repository → PostgreSQL path. Integration tests will catch wiring bugs, SQL errors, and cross-layer validation logic that unit tests can't reach.

---

## Technical setup

| Decision | Choice | Reason |
|---|---|---|
| Spring context | `@SpringBootTest(webEnvironment = MOCK)` | Full context + MockMvc. MOCK keeps the servlet call on the same thread as the test, making `@Transactional` rollback reliable. |
| Database | Testcontainers (`org.testcontainers:postgresql`) | Real Postgres, isolated per test run, no shared state with local dev DB. |
| Schema init | Reuse `classpath:/db/schema.sql` via `spring.sql.init.*` | Schema already uses `CREATE TABLE IF NOT EXISTS` — idempotent, no Flyway needed. |
| Data isolation | `@Transactional` on each test | MOCK env keeps HTTP calls on the same thread → all JDBC ops join the test transaction → rollback at end. No manual truncate needed. |
| HTTP client | `MockMvc` (auto-configured with `@AutoConfigureMockMvc`) | Already in use for unit tests; consistent style. |

### How `@Transactional` isolation works here

With `MOCK` env, `MockMvc.perform(...)` calls `DispatcherServlet` synchronously on the same thread. `JdbcTemplate` uses `DataSourceUtils.getConnection(dataSource)`, which returns the connection already bound to the test transaction. All SQL — inserts, selects, deletes — are visible within that transaction, and the whole thing rolls back when the test method exits. Tests never see each other's data.

### New dependencies needed in `build.gradle`

```groovy
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:postgresql'
```

---

## Assumptions

1. **No cascade deletes.** `expenses.account_id` has a FK to `accounts` with no `ON DELETE CASCADE`. Attempting to delete an account that has expenses will produce a `DataIntegrityViolationException` → caught by `GlobalExceptionHandler.handleDatabase` → `500 DATABASE_ERROR`. This is tested explicitly.

2. **`initialAmount` sets both columns on create.** `AccountRepository.create` inserts `initial_amount` and `current_amount` both from `entity.initialAmount()`. On update, only `current_amount` changes. Tests will assert this.

3. **Expense creation requires an account.** `ExpenseService` calls `accountService.getById(expense.accountId())` before inserting. This means every expense test needs a pre-existing account. Setup is handled inside the same `@Transactional` test (create account via `MockMvc`, then create expense), so both are rolled back together.

4. **Ownership is enforced at the service layer.** `ExpenseService.expenseWriteValidations` compares `expense.createdBy()` with `account.createdBy()`. If they differ → `UnauthorizedAccountException` → `403 FORBIDDEN`. This is tested as a full-stack flow (account created for `user-A`, expense attempted by `user-B`).

5. **Request bodies follow the `{ "value": { ... } }` wrapper pattern** for accounts and expenses. Flat JSON for accounts will not deserialize correctly — tests use the correct shape.

6. **`ExpenseController` only exposes `POST /expenses` today.** `ExpenseRepository` has `findByUser`, `findByAccount`, `findByCategory`, `findBySubcategory`, `update`, and `delete` — none wired to HTTP yet. Integration tests cover only the current API surface. Test classes are structured to make adding future endpoint tests straightforward.

7. **Pagination defaults.** `GET /accounts/user/{userId}` defaults to `page=0, size=20`. Tests that verify pagination use explicit params.

8. **No auth layer.** All endpoints are open. No token setup needed in tests.

---

## Open questions

These are not blocking — the spec records the decision made for each, but they should be confirmed before implementation.

| Question | Decision taken |
|---|---|
| Should expense tests set up prerequisite accounts via MockMvc or direct JDBC injection? | Via MockMvc — keeps the full create path exercised and stays within the `@Transactional` boundary. |
| Should we test the FK violation (delete account with expenses)? | Yes — it's a real edge case with a defined error shape (`500 DATABASE_ERROR`). |
| Should the base class be abstract or use a JUnit 5 extension? | Abstract base class — simpler, no extra dependency. |
| Test class naming: `*IT` or `*IntegrationTest`? | `*IT` — Maven/Gradle Failsafe convention; also clearly distinct from `*Test` unit tests. |

---

## File structure

```
src/test/java/com/novelosoftware/expenses/
├── BaseIT.java                          ← shared: Testcontainers, @SpringBootTest, @Transactional
├── controllers/
│   ├── AccountControllerIT.java
│   └── ExpenseControllerIT.java
```

---

## Test inventory

### `AccountControllerIT`

Each test is `@Transactional` via `BaseIT`. All data rolled back after each test.

#### POST /accounts

| # | Scenario | Setup | Request body | Expected |
|---|---|---|---|---|
| A1 | Happy path — creates account | none | valid account (DEBIT, USD, 1000.00) | 201, body has `accountId`, `initialAmount == currentAmount == 1000.00` |
| A2 | Missing name | none | `name: ""` | 400, `code: BAD_REQUEST`, message contains "Account name" |
| A3 | Missing accountType | none | `accountType: null` | 400, `code: BAD_REQUEST` |
| A4 | Invalid enum value for accountType | none | `accountType: "SAVINGS"` | 400, message contains accepted values (DEBIT, CREDIT) |
| A5 | Malformed JSON | none | `{ invalid }` | 400, `code: BAD_REQUEST`, message: "Malformed request body" |

#### GET /accounts/{id}

| # | Scenario | Setup | Expected |
|---|---|---|---|
| A6 | Found | create account → use returned id | 200, body matches created account |
| A7 | Not found | none (id 999999) | 404, `code: NOT_FOUND` |

#### GET /accounts/user/{userId}

| # | Scenario | Setup | Expected |
|---|---|---|---|
| A8 | Returns accounts for user | create 2 accounts for `user-it` | 200, `totalElements: 2`, `content` has 2 items |
| A9 | Empty for unknown user | none | 200, `totalElements: 0`, `content: []` |
| A10 | Pagination — size=1, page=0 | create 2 accounts | `totalPages: 2`, `content` has 1 item |
| A11 | Pagination — size=1, page=1 | create 2 accounts | `content` has 1 item (second account) |
| A12 | User isolation | create account for `user-A`, query for `user-B` | `totalElements: 0` |

#### PUT /accounts/{id}

| # | Scenario | Setup | Expected |
|---|---|---|---|
| A13 | Happy path — updates account | create account | 200, body reflects new values, `initialAmount` unchanged |
| A14 | Not found | none (id 999999) | 404, `code: NOT_FOUND` |
| A15 | Missing name | create account | 400, `code: BAD_REQUEST` |

#### DELETE /accounts/{id}

| # | Scenario | Setup | Expected |
|---|---|---|---|
| A16 | Happy path — deletes account | create account | 204, subsequent GET returns 404 |
| A17 | Not found | none (id 999999) | 404, `code: NOT_FOUND` |
| A18 | FK violation — account has expenses | create account + create expense against it | 500, `code: DATABASE_ERROR` |

---

### `ExpenseControllerIT`

Each test is `@Transactional` via `BaseIT`. Prerequisite account is created via MockMvc inside the same test or `@BeforeEach`.

**Prerequisite account payload (reused across expense tests):**
```json
{
  "value": {
    "name": "Test Account",
    "accountType": "DEBIT",
    "currency": "USD",
    "initialAmount": 5000.00,
    "createdBy": "user-expense-it"
  }
}
```

**Valid expense payload (template):**
```json
{
  "value": {
    "expenseDate": "2026-05-27",
    "accountId": <id from setup>,
    "amount": 42.50,
    "description": "Test tacos",
    "subCategory": "RESTAURANT",
    "createdBy": "user-expense-it"
  }
}
```

#### POST /expenses

| # | Scenario | Setup | Request | Expected |
|---|---|---|---|---|
| E1 | Happy path | create account for `user-expense-it` | valid expense with same `createdBy` | 201, body has `expenseId`, `expenseDate`, `accountId`, `amount`, `subCategory: RESTAURANT` |
| E2 | Account does not exist | none | `accountId: 999999` | 404, `code: NOT_FOUND` |
| E3 | Account belongs to different user | create account for `user-A` | expense with `createdBy: user-B` | 403, `code: FORBIDDEN` |
| E4 | Missing expenseDate | create account | `expenseDate: null` | 400, `code: BAD_REQUEST`, message: "expenseDate cannot be null" |
| E5 | Missing accountId | create account | `accountId: null` | 400, `code: BAD_REQUEST` |
| E6 | Missing amount | create account | `amount: null` | 400, `code: BAD_REQUEST` |
| E7 | Missing description | create account | `description: ""` | 400, `code: BAD_REQUEST` |
| E8 | Missing subCategory | create account | `subCategory: null` | 400, `code: BAD_REQUEST` |
| E9 | Missing createdBy | create account | `createdBy: ""` | 400, `code: BAD_REQUEST` |
| E10 | Invalid subCategory enum value | create account | `subCategory: "INVALID_ENUM"` | 400, message lists accepted values |
| E11 | Malformed JSON | create account | unclosed brace | 400, `code: BAD_REQUEST`, message: "Malformed request body" |

---

## Notable multi-step flows

These tests are worth calling out because they exercise state transitions across multiple HTTP calls within one transaction.

| Flow | Steps | What it proves |
|---|---|---|
| Create → Read → Verify | POST account → GET by id → assert fields match | Full round-trip: serialization, SQL insert, SQL select |
| Create → Update → Read | POST account → PUT account → GET by id → assert updated fields, `initialAmount` unchanged | Update does not clobber `initial_amount` |
| Create → Delete → Confirm gone | POST account → DELETE → GET → 404 | Delete removes the row; subsequent fetch correctly returns 404 |
| Create account + expense → Delete account | POST account → POST expense → DELETE account → 500 DATABASE_ERROR | FK constraint surface, error handler coverage |
| Create expense → verify category derived | POST account → POST expense with `subCategory: RESTAURANT` → assert `subCategory` in response | `CategoryMapper` → DB → `ExpenseMapper` round-trip |

---

## Response shapes for reference

**Error body (all 4xx/5xx):**
```json
{ "code": "NOT_FOUND", "message": "Account with ID 99 not found." }
```

**Account body:**
```json
{
  "accountId": 1,
  "name": "Test Account",
  "accountType": "DEBIT",
  "currency": "USD",
  "initialAmount": 1000.00,
  "currentAmount": 1000.00,
  "createdBy": "user-it"
}
```

**Expense body:**
```json
{
  "expenseId": 1,
  "expenseDate": "2026-05-27",
  "accountId": 1,
  "amount": 42.50,
  "description": "Test tacos",
  "subCategory": "RESTAURANT",
  "createdBy": "user-expense-it"
}
```

---

## Out of scope

- `ExpenseRepository` finder methods (`findByUser`, `findByAccount`, `findByCategory`, `findBySubcategory`) — not yet exposed via HTTP. When endpoints are added, matching `*IT` test cases should be added following the same pattern.
- `ExpenseRepository.update` and `ExpenseRepository.delete` — same reason.
- Performance / load testing.
- Security / auth testing (no auth layer exists).
