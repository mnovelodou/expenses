## Context

The expense list endpoint (`GET /expenses`) currently supports only `userId` + date range filtering, backed by `idx_expenses_user_date (created_by, expense_date DESC, expense_id DESC)`. The repository layer has grown a set of per-filter methods (by category, subcategory, account) that all use offset pagination and are not connected to the cursor-based path. These methods have supporting indexes that are now redundant given the mandatory date range constraint.

The goal is to unify filtering under a single cursor-paginated path and remove the dead code and indexes.

## Goals / Non-Goals

**Goals:**
- Add optional `category`, `subcategory`, `accountId` filters to the cursor-paginated list endpoint
- Support any combination of the three optional filters in a single query
- Remove the per-filter repository methods and their indexes
- Keep `userId` + date range mandatory (preserves index efficiency)

**Non-Goals:**
- Full-text search on `description`
- Multiple values per filter (e.g. `category=Food,Travel`)
- Backward pagination
- Changing the cursor encoding format

## Decisions

### 1. Single criteria builder method replaces all per-filter methods

**Decision:** Replace `findByCategory`, `findBySubcategory`, `findByAccount`, `findByUser` (offset), and their `countBy*` counterparts with one method — `findByFiltersCursor` — that dynamically builds the WHERE clause.

**Rationale:** With 3 optional filters, the combinatorial explosion is 7 method variants. A criteria builder handles all combinations in one place and naturally extends if a new filter is added later. Spring's `JdbcTemplate` supports parameterized dynamic SQL safely (no string interpolation of values).

**Alternative considered:** Keep separate methods and add cursor variants for each. Rejected — doubles the method count and still doesn't handle combinations.

---

### 2. `idx_expenses_user_date` is sufficient — three indexes removed

**Decision:** Remove `idx_expenses_category_date`, `idx_expenses_subcategory_date`, and `idx_expenses_account_date` from `schema.sql`. Do not add DROP statements; simply remove the CREATE INDEX lines.

**Rationale:** With `created_by` + date range mandatory, the planner uses `idx_expenses_user_date` to bound the scan to a small set of rows, then applies the optional filter predicates. For a personal finance dataset with a bounded date window (max 3 months), this is efficient. The removed indexes only help single-filter queries and don't cover combinations; the criteria builder approach makes them redundant.

**Risk:** If the date range constraint is ever relaxed, query cost increases. Mitigated by keeping the `DateWindowResolver` enforcement in place.

---

### 3. Filters are stateless — cursor does not encode filter state

**Decision:** The cursor encodes only `(expense_date, expense_id)`. Callers must re-supply the same filters on every page request.

**Rationale:** Encoding filter state in the cursor adds complexity and size for minimal benefit. This is the standard pattern for cursor pagination.

**API contract consequence:** The controller must validate that the cursor date falls within the requested date window (already done for the current `listByUser` path).

---

### 4. Filters passed as individual nullable parameters — no wrapper object

**Decision:** Pass `category`, `subcategory`, and `accountId` as individual nullable parameters through controller → service → repository. No `ExpenseFilters` wrapper DTO.

**Rationale:** Three parameters don't justify a wrapper object. Individual params keep the call signatures explicit and avoid an extra class with no real behaviour. If more filters are added in the future, a wrapper can be introduced then.

## Risks / Trade-offs

- **[Risk] Planner falls back to seq scan if date range guard is bypassed** → `DateWindowResolver` enforces the 3-month cap at the service layer; the repository itself also receives explicit start/end dates.
- **[Risk] Dynamic SQL is harder to read in logs** → Mitigated by keeping the builder logic simple and well-commented.
- **[Trade-off] Removing per-filter indexes slightly hurts single-filter queries on large datasets** → Acceptable for the current scale; index can be re-added if profiling shows a need.
