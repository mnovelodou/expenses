## Context

`initialAmount` represents the opening balance of an account. Currently it is set on creation and excluded from the `UPDATE` SQL, making it permanently immutable. The `current_amount` tracks the live balance (initial + all transactions). When `initialAmount` changes, `current_amount` must be adjusted by the same delta so existing transactions remain accurate.

## Goals / Non-Goals

**Goals:**
- Allow `initialAmount` to be updated via `PUT /accounts/{id}`.
- Keep `current_amount` consistent: apply the delta (`new - old`) to `current_amount` on every update.
- Cover the new behaviour with unit tests and integration tests.

**Non-Goals:**
- No new endpoint; the change reuses `PUT /accounts/{id}`.
- No migration of historical data; existing rows are unaffected.
- No audit trail for `initialAmount` changes.

## Decisions

**Delta adjustment for `current_amount`**
When `initialAmount` changes from `old` to `new`, apply `current_amount += (new - old)`. This preserves the effect of all recorded transactions without touching any transaction records.

Alternative considered: recalculate `current_amount` from scratch by summing transactions. Rejected because the expenses table may not always capture every historical transaction, and it adds a second query with no benefit for the common case.

**Where to compute the delta**
The delta is computed in `AccountService.update`, not the repository. The service fetches the existing entity first (already done for the "not found" check), so `oldInitialAmount` is available there before the update query runs. The repository receives the final `currentAmount` to persist.

**SQL change**
Add `initial_amount = ?` to the `UPDATE` SET clause and pass the new `initialAmount` as a parameter. No schema migration needed; the column already exists.

## Risks / Trade-offs

- [Concurrent updates] Two simultaneous updates could produce an inconsistent `current_amount`. → Acceptable for now; no concurrent-write SLA exists on this service. A future optimistic-lock column would address this.

## Migration Plan

No schema changes. Deploy the updated service. Existing accounts are unaffected until they receive an update request that changes `initialAmount`.
