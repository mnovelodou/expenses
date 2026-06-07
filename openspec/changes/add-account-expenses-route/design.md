## Context

`GET /expenses?account_id={id}` already supports filtering by account, but requires callers to also pass `user_id`. The nested route `GET /accounts/{id}/expenses` can resolve the owner internally by loading the account, making it a more ergonomic shortcut without duplicating any query logic.

The stack is Spring Boot with `JdbcTemplate`. All expense queries go through `ExpenseService.listByUser`, which requires a `userId`. `AccountService.getById` can supply that value from the account record's `createdBy` field.

## Goals / Non-Goals

**Goals:**
- Add `GET /accounts/{id}/expenses` with full filter/pagination parity to `GET /expenses`
- Resolve `user_id` automatically from the account — callers do not pass it
- Return 404 when the account does not exist

**Non-Goals:**
- Replacing or deprecating `GET /expenses?account_id=`
- Cross-user expense queries (the endpoint is scoped to the account owner)
- Any new query logic — this is a routing convenience on top of existing service code

## Decisions

**Resolve user from account, not from a request param**: Callers of this route already know which account they want; requiring them to also pass `user_id` would be redundant. `AccountService.getById` throws `AccountNotFoundException` (404) if the account is absent, which is the correct error for a bad path param.

**Place the endpoint on `AccountController`**: The path is `/accounts/{id}/expenses`, so it lives naturally in `AccountController`. It injects `ExpenseService` (or delegates through a shared service call) to keep the boundary clear.

**Reuse `ExpenseService.listByUser` unchanged**: The method already accepts `accountId` as an optional filter and `userId` as mandatory. We pass both — `userId` from the resolved account, `accountId` from the path — so the DB query is identical to the existing filter path. No new repository method needed.

## Risks / Trade-offs

- [Tight coupling between AccountController and ExpenseService] AccountController now depends on ExpenseService → Acceptable; the dependency is one-directional and the use case is well-defined. Alternative (a dedicated `AccountExpenseService`) would be over-engineering for a single delegating method.

## Migration Plan

No schema changes. Deploy is additive — existing clients are unaffected.
