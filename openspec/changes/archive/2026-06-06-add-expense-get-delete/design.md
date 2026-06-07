## Context

The expenses API has `POST /expenses` (create) and `PUT /expenses/{id}` (update) but is missing `GET /expenses/{id}` and `DELETE /expenses/{id}`. The existing stack is Spring Boot with `spring-boot-starter-jdbc`; all queries are plain SQL executed via `JdbcTemplate`. Notably, `ExpenseRepository` already has `get(Long id)` and `delete(Long id)` implemented — only the service and controller layers need to be added.

## Goals / Non-Goals

**Goals:**
- Add `GET /expenses/{id}` returning the full `Expense` DTO
- Add `DELETE /expenses/{id}` returning 204 No Content
- Return 404 when the requested ID does not exist (both endpoints)

**Non-Goals:**
- Soft delete / audit trail (permanent hard delete only)
- Bulk delete
- Authorization checks beyond what the existing endpoints enforce

## Decisions

**Reuse `ExpenseNotFoundException`**: The existing `ExpenseServiceExceptions` pattern (e.g. for update) will be extended with a not-found exception that the `GlobalExceptionHandler` maps to 404. No new exception infrastructure needed.

**No new DTO**: `GET /expenses/{id}` returns the existing `Expense` record directly (same shape as list items). Wrapping it in a response object would be inconsistent with the list endpoint and add noise.

**Hard delete**: The data model has no `deleted_at` column and there is no stated requirement for audit history, so a hard `DELETE` via `deleteById` is the right call. Soft delete can be added later if needed.

## Risks / Trade-offs

- [Orphaned references] Deleting an expense that is referenced by external systems (reports, exports) will produce dangling IDs → Acceptable at this stage; callers are expected to handle 404 on stale IDs.

## Migration Plan

No schema changes required. Deploy is a drop-in addition of two endpoints.
