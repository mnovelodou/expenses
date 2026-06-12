## Why

When creating an account, the caller may provide a `currentAmount` that differs from `initialAmount` — for example, when importing an account that already has a transaction history. Currently `AccountMapper.toEntity(CreateAccountRequest)` ignores the supplied `currentAmount` and forces it to equal `initialAmount`, making it impossible to set an independent starting balance at creation time.

## What Changes

- `AccountMapper.toEntity(CreateAccountRequest)` will use the caller-supplied `currentAmount` (defaulting to `initialAmount` when not provided, preserving existing behavior for clients that omit it).
- The `POST /accounts` endpoint will now honour `value.currentAmount` in the request body.

## Capabilities

### New Capabilities

_(none — this is a bug fix on an existing capability)_

### Modified Capabilities

- `create-account`: `currentAmount` in the creation request is now respected instead of being silently overwritten with `initialAmount`.

## Impact

- `AccountMapper` (mapper layer)
- `AccountRepository.create` — passes `entity.currentAmount()` to the INSERT; this already works correctly once the mapper is fixed
- `POST /accounts` contract — additive: clients that omit `currentAmount` are unaffected; clients that supply it now see it reflected in the response
- Existing tests that assert `currentAmount == initialAmount` at creation may need updating if they exercise the new path
