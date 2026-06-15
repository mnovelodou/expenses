## Context

Accounts currently store `initial_amount` and `current_amount` as manually maintained values. Users copy their bank balance into `current_amount` periodically and log expenses. There is no concept of a tracking period, so the API cannot compute how much of the balance delta is still unexplained by logged expenses (the "gap").

The project uses plain JDBC with `JdbcTemplate` — no JPA or ORM. Schema changes require SQL migration files under `src/main/resources/db/`. Live data exists in production; migrations must be additive.

## Goals / Non-Goals

**Goals:**
- Add `period_start` to accounts with a backward-compatible migration
- Expose `period_start` in all account read/write endpoints
- Compute gap on demand (opt-in) without affecting existing response contracts

**Non-Goals:**
- Historical period ranges (future `period_end`)
- Auto-advancing `period_start` at month boundaries
- Filtering/querying accounts by `period_start`

## Decisions

### `period_start` column is nullable in DB, required on create
Existing rows must not break. Making it nullable at the DB level with a NOT NULL constraint enforced only in application code on `POST` is the cleanest migration path. `PATCH` silently ignores null to support partial updates.

*Alternative considered*: backfill existing rows with a default date — rejected because we don't know what the correct date would be for each existing account.

### Gap is opt-in via `?includeGap=true`
Gap requires an aggregation query over the expenses table. Including it unconditionally on every account fetch would add latency for callers that don't need it (e.g., listing accounts for a dropdown). An explicit flag keeps the default path cheap.

*Alternative considered*: separate `GET /accounts/{id}/gap` endpoint — rejected because the gap is a derived property of the account, not a separate resource, and `?includeGap=true` is simpler to consume.

### Gap returns `null` when `period_start` is null
Rather than erroring or defaulting, returning `null` communicates "this account has no period configured yet." Clients can display this state appropriately.

### Column name `period_start` (not `start_date`)
Chosen to semantically pair with a future `period_end` for historical reporting. Signals intent without requiring a rename later.

## Risks / Trade-offs

- **Existing accounts have no `period_start`**: Gap will always be `null` for them until updated. → Acceptable; clients must handle null.
- **Gap is point-in-time**: Two requests can return different gaps if expenses are added between them. → Expected behavior, not a bug.
- **No validation that expenses belong to correct user**: Gap query is scoped by `account_id` which already belongs to the authenticated user. → Sufficient.

## Migration Plan

1. Add SQL migration file: `ALTER TABLE accounts ADD COLUMN period_start DATE;` (nullable, no default)
2. Deploy — existing rows have `period_start = NULL`, no downtime risk
3. Clients can begin setting `period_start` on new accounts immediately
4. Existing accounts can be updated via `PATCH` to set a `period_start`

**Rollback**: `ALTER TABLE accounts DROP COLUMN period_start;` — safe since no existing queries depend on it before deploy.
