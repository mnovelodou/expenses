## Why

The `initialAmount` field on an account is currently immutable after creation — the `UPDATE` SQL excludes it, so clients cannot correct it if it was set incorrectly. This limits account management and forces users to delete and recreate accounts to fix a wrong opening balance.

## What Changes

- The `AccountRepository.update` SQL will include `initial_amount` so it is persisted on update.
- `AccountService.update` will persist the new `initialAmount`; `currentAmount` remains independently controlled (caller-provided value if present, otherwise the stored value is preserved).
- Unit tests for `AccountService` and `AccountRepository` will cover the new behavior.
- Integration tests for `AccountController` will verify the field is accepted and persisted end-to-end.

## Capabilities

### New Capabilities

_(none — this is a behaviour change to an existing capability)_

### Modified Capabilities

- `account-management`: `initialAmount` becomes mutable via `PUT /accounts/{id}`; `currentAmount` remains independently controlled and is not affected by changes to `initialAmount`.

## Impact

- `AccountRepository.update` SQL query
- `AccountService.update` logic (null-coalescing for `initialAmount` and `currentAmount`)
- Unit tests: `AccountServiceTest`, `AccountRepositoryTest` (or equivalent)
- Integration tests: `AccountControllerIT`
