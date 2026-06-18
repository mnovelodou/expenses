## Context

Expenses are stored in a single Postgres `expenses` table accessed through hand-written SQL on `JdbcTemplate` (no JPA). The list endpoint `GET /expenses` runs a criteria search in `ExpenseRepository.findByFiltersCursor` that mandates `created_by` + a date range (bounded by `idx_expenses_user_date`) and applies optional `category`/`subcategory`/`account_id` predicates on top, with keyset (cursor) pagination ordered by `expense_date DESC, expense_id DESC`.

We need to record the original transaction total on each expense line so that lines produced by splitting a single transaction can later be found together. The user has explicitly deferred adding a database index for the new column.

## Goals / Non-Goals

**Goals:**
- Add a nullable `transaction_amount` column to `expenses` and surface it through the entity, DTO, and mapper.
- Persist it on create, bulk-create, and update.
- Add an exact-match `transaction_amount` filter to the existing criteria search, reusing the current cursor pagination.

**Non-Goals:**
- No index on `transaction_amount` in this change (the user will decide later).
- No automatic split workflow / endpoint — callers set `transactionAmount` themselves on each line.
- The system never derives `transactionAmount` from `amount` changes or split logic; the only "computed" behavior is defaulting a null request value to the line's `amount`.
- No range or fuzzy matching on `transaction_amount` (exact equality only).
- No backfill of `transaction_amount` for existing rows (they remain `null`).

## Decisions

- **Nullable column, defaulted to `amount` only at creation.** `transaction_amount NUMERIC(15,2)` is nullable in the schema (so existing rows and the column add stay simple). On the **create** and **bulk-create** paths the service resolves a null request value to the line's `amount` before persisting, so a freshly created un-split expense naturally has `transactionAmount == amount`. The default is applied in `ExpenseService`, not in SQL, so the resolved value is reflected in the response. Alternative considered: a SQL `DEFAULT`/`COALESCE` — rejected because it wouldn't round-trip the resolved value into the returned DTO and would split the rule across layers.
- **Update is partial for `amount` and `transactionAmount`.** `PUT /expenses/{id}` already fetches the existing entity (for ownership checks). The update path SHALL merge nulls from the stored row: a null `amount` keeps the stored `amount` (no validation error), and a null `transactionAmount` keeps the stored `transactionAmount` (it is NOT re-defaulted to `amount`). The create-time "null → amount" default does not run on update. Rationale: the user wants update to change a field only when explicitly provided. This requires relaxing the current `expenseWriteValidations`, which rejects a null `amount` — on the update path, `amount` (and `transactionAmount`) become optional and are back-filled from the persisted entity before the SQL `UPDATE`.
- **Default is never derived from splits.** The service only substitutes `amount` for a null `transactionAmount` at creation; it never recomputes it from `amount` edits or split operations. Callers that split a transaction are responsible for sending the original total on each line.
- **Additive migration in `schema.sql`.** Follow the existing pattern (`ALTER TABLE ... ADD COLUMN IF NOT EXISTS`) used for `accounts.period_start`. No index statement, per the user's instruction.
- **Exact-equality filter threaded through the existing method.** Add a `BigDecimal transactionAmount` parameter to `findByFiltersCursor` and append `AND transaction_amount = ?` when non-null — identical in shape to the existing optional predicates. Rationale: reuse the proven criteria + keyset path rather than a parallel query. Alternative considered: a dedicated finder method — rejected as duplicative.
- **Filter not encoded in the cursor.** Consistent with `category`/`subcategory`/`accountId`, the caller resupplies `transaction_amount` on every page. The cursor stays `(expense_date, expense_id)`.
- **Equality on NUMERIC.** Postgres compares `NUMERIC` by value, so `100.0 = 100.00`. Binding a `BigDecimal` avoids scale surprises; no special normalization needed.

## Risks / Trade-offs

- **Unindexed filter scans the date-bounded slice** → Acceptable: the mandatory `created_by` + date-range predicates already bound the scan via `idx_expenses_user_date`; `transaction_amount` is an in-memory filter over that slice. An index can be added later if needed.
- **Float/scale mismatch from JSON clients** → Binding `BigDecimal` and relying on Postgres NUMERIC value-equality avoids `100.0` vs `100.00` mismatches.
- **Column added to `RETURNING *` / `SELECT *` rows** → The `RowMapper` must read the new column; updating it and the insert/update column lists together keeps reads and writes consistent.

## Migration Plan

1. Apply the additive `ALTER TABLE expenses ADD COLUMN IF NOT EXISTS transaction_amount NUMERIC(15,2)` (idempotent, runs on startup with the rest of `schema.sql`).
2. Deploy code that reads/writes the column and defaults null requests to `amount`. Existing rows keep `transaction_amount = null` (no backfill); newly written rows always carry a value; existing clients that don't send the field get `transactionAmount == amount` transparently.
3. Rollback: revert the code; the nullable column can remain in place harmlessly (no drop required).

## Open Questions

- Whether to later add `idx_expenses_user_date_txamount` or a partial index once query volume on the filter is understood — deferred by the user.
