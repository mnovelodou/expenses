## Context

The expenses service currently supports creating and updating expenses but has no read/list API. The repository layer already has `findByUser` with offset pagination and `ORDER BY expense_date DESC`, but there is no controller endpoint exposing it. This change introduces cursor-based pagination from the start to avoid the scalability problems of offset pagination as expense history grows.

## Goals / Non-Goals

**Goals:**
- Add `GET /expenses` with a required `user_id` query parameter; scoped to that user's expenses
- Optional date window defaulting to last calendar month; 3-month cap enforced via `Period.ofMonths(3)`
- Stable, tie-broken sort order (`expense_date DESC, expense_id DESC`)
- Opaque cursor so the pagination mechanism can evolve without client changes

**Non-Goals:**
- Backward (prev) cursor pagination — deferred
- Filtering by account, category, or subcategory — separate change
- Total count in the response — incompatible with cursor pagination goals

## Decisions

### Cursor encoding: opaque base64 token
The cursor encodes `{ "d": "<expense_date>", "id": <expense_id> }` as a base64 string. Clients treat it as an opaque token and echo it back verbatim.

**Alternatives considered:**
- Transparent query params (`cursor_date` + `cursor_id`): easier to debug but locks the API to the current cursor structure; any internal change becomes a breaking change.

### Composite cursor position: `(expense_date, expense_id)`
`expense_date` alone is not unique — multiple expenses can share the same date. Adding `expense_id` as a tie-breaker guarantees a stable, unambiguous cursor position.

The SQL predicate expands the tuple comparison for index compatibility:
```sql
AND (expense_date < :d OR (expense_date = :d AND expense_id < :id))
```

### New DTO: `CursorPageResponse<T>`
A separate DTO avoids polluting `PageResponse` (which carries `totalElements`, `totalPages`) with cursor semantics. `CursorPageResponse<T>` has: `content`, `nextCursor` (nullable), `pageSize`.

### Index: add `expense_id DESC` as trailing column
The existing `idx_expenses_user_date ON expenses(created_by, expense_date)` is updated to `(created_by, expense_date DESC, expense_id DESC)`. This covers the ORDER BY columns fully, eliminating a heap fetch for the `expense_id` tie-breaker on each result row.

The same pattern should be applied to the other query indexes when those endpoints are added.

### User identification: explicit parameter until OAuth
`user_id` is a required query parameter for now. This mirrors the existing pattern in the codebase (`created_by TEXT DEFAULT 'default_user_id'`). When OAuth is introduced, `user_id` will be extracted from the token server-side and this parameter will be dropped — no data model change is needed, only the resolution point moves.

### Date window validation
- `start_date` and `end_date` are independently optional. Each absent param is defaulted using calendar arithmetic: both absent → previous calendar month; only `start_date` → `end_date = start_date + 1 month`; only `end_date` → `start_date = end_date - 1 month`. Use date library month arithmetic, not day counts.
- Range cap: the resolved range SHALL NOT exceed 3 calendar months; use date library month-aware comparison, not a fixed day count. → 400 if exceeded.
- `end_date < start_date` → 400.

## Risks / Trade-offs

- **Cursor opacity vs. debuggability** → Mitigation: during development the token is just base64 JSON, easily decoded in a terminal.
- **No total count** → clients cannot show "page X of Y" UI; they can only show "load more." This is intentional and consistent with cursor pagination.
- **Index recreation is a DDL operation** → No production data yet, so this is safe. Future: would need a concurrent index rebuild.

## Resolved Questions

- **`limit` cap**: Enforce a server-side maximum of 100. Requests above the cap are rejected with 400 — not clamped, to avoid silently returning fewer results than requested (which could be misread as the last page).
- **Cursor validation**: No signature or tampering check needed. The only validation is: (1) the token must be decodable — 400 if not; (2) the decoded date must fall within the request's date range — 400 if not. A structurally valid cursor whose date is within range but points to an unexpected position simply returns the page at that position, which is the user's own data and harmless.
