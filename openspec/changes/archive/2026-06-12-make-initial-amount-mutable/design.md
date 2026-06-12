## Context

`initialAmount` represents the opening balance of an account. Currently it is set on creation and excluded from the `UPDATE` SQL, making it permanently immutable. `currentAmount` is an independently managed field; clients send the desired value on every update.

## Goals / Non-Goals

**Goals:**
- Allow `initialAmount` to be updated via `PUT /accounts/{id}`.
- `currentAmount` remains independently controlled — unchanged by modifications to `initialAmount`.
- Cover the new behaviour with unit tests and integration tests.

**Non-Goals:**
- No new endpoint; the change reuses `PUT /accounts/{id}`.
- No migration of historical data; existing rows are unaffected.
- No audit trail for `initialAmount` changes.

## Decisions

**`initialAmount` and `currentAmount` are independent**
Both fields follow the same null-coalescing rule: use the caller-provided value when present, otherwise preserve the stored value. There is no derived relationship between them — changing `initialAmount` has no effect on `currentAmount`.

**SQL change**
Add `initial_amount = ?` to the `UPDATE` SET clause and pass the resolved `initialAmount` as a parameter. No schema migration needed; the column already exists.

**Where to resolve null fields**
`AccountService.update` fetches the existing entity (already required for the not-found check), resolves both fields via null-coalescing, and passes the fully populated entity to the repository.

## Risks / Trade-offs

- [Concurrent updates] Two simultaneous updates could produce stale reads for the null-coalescing fallback. → Acceptable for now; no concurrent-write SLA exists on this service.

## Migration Plan

No schema changes. Deploy the updated service. Existing accounts are unaffected until they receive an update request.
