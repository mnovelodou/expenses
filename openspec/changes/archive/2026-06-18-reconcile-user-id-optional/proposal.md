## Why

The `list-expenses-by-user` and `expense-search-filters` specs still describe `user_id` as a **required** query parameter whose absence returns HTTP 400. That contract is stale: owner-scoped-authorization already made `user_id` **optional**, defaulting it to the authenticated caller's `sub` (a supplied non-matching user is hidden as 404). The controller declares `user_id` as `required = false` and `ExpenseService.listByUser` implements the default-to-caller behavior. The `owner-scoped-authorization` spec already documents the correct contract, so the two older specs now contradict both the code and that spec.

## What Changes

- Reconcile the `list-expenses-by-user` spec so `user_id` is documented as optional and defaulting to the caller, and replace the "missing user_id → 400" scenario with a "missing user_id → defaults to caller (200)" scenario.
- Reconcile the `expense-search-filters` spec so it no longer claims `userId` is mandatory; only the date range remains mandatory.
- No code changes — this aligns the documented contract with the already-implemented behavior.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `list-expenses-by-user`: `user_id` becomes documented as optional/default-to-caller; the missing-parameter outcome changes from 400 to 200 with the caller's expenses.
- `expense-search-filters`: the "userId mandatory" statement is corrected; date range remains mandatory, `user_id` optional.

## Impact

- Specs only: `openspec/specs/list-expenses-by-user/spec.md` and `openspec/specs/expense-search-filters/spec.md`.
- No production or test code changes; behavior already matches the corrected specs.
- Removes the contradiction CODEX flagged on PR #30.
