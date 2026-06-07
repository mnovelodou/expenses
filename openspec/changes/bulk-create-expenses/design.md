## Context

The expenses API currently supports creating a single expense via `POST /expenses`. Importing historical data (e.g., a CSV export from a bank) or syncing a bank feed requires calling this endpoint once per expense, resulting in N round trips for N expenses. A bulk endpoint eliminates this overhead.

The existing service and repository layers use plain JDBC (`JdbcTemplate`) with hand-written SQL — no JPA/ORM. The bulk insert should follow the same pattern.

## Goals / Non-Goals

**Goals:**
- Single endpoint to create multiple expenses in one HTTP call
- Atomic operation: all succeed or all fail (single transaction)
- Reuse existing expense validation logic
- Return created expenses (with server-assigned IDs and timestamps)

**Non-Goals:**
- Partial success / per-item error reporting (out of scope for v1)
- Async/background processing — response is synchronous
- Idempotency keys or duplicate detection
- Streaming or chunked uploads

## Decisions

### Endpoint: `POST /expenses/bulk`
Separate route from `POST /expenses` to keep the single-create contract unchanged. Avoids versioning the existing endpoint.

Alternative considered: accept both single object and array at `POST /expenses` based on content shape — rejected because it complicates request parsing and OpenAPI schema definition.

### Request body: array of expense inputs
```json
{ "expenses": [ { ...ExpenseInput }, ... ] }
```
Wrapped object (rather than a bare array) is easier to extend later (e.g., add top-level options) and plays nicer with some HTTP frameworks and proxies.

### Atomicity: single transaction wrapping all inserts
Validation failures return 400/422 and nothing is persisted. Database/internal errors during insert return 500 and the transaction is rolled back. In both cases there are no partial commits.

### Batch insert strategy: JDBC batch update
Use `JdbcTemplate.batchUpdate` with a `BatchPreparedStatementSetter`. This is efficient for moderate batch sizes and consistent with the existing JDBC-only approach.

### Size limit: cap at 200 items per request
Prevents runaway memory usage and overly long transactions. Requests exceeding this limit return HTTP 400 with a descriptive message.

### Response: array of created expenses
Return the same shape as `POST /expenses` per item, wrapped in `{ "expenses": [...] }`. Gives the caller the server-assigned IDs without a follow-up query.

## Risks / Trade-offs

- **Large batches hold a DB transaction open longer** → Mitigation: enforce the 200-item cap; document the limit in the API spec.
- **Validation errors report the whole request as failed, not per-item** → Mitigation: document clearly; per-item errors can be added in a future iteration.
- **Sequential ID assignment may not match input order** → Mitigation: response preserves insertion order so callers can correlate by position.
