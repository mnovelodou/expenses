## Context

`POST /accounts` accepts a JSON body with a `value` object that includes both `initialAmount` and `currentAmount`. The mapper `AccountMapper.toEntity(CreateAccountRequest)` currently ignores `currentAmount` and substitutes `initialAmount` for both fields. The fix is a one-line change in the mapper; the repository INSERT already passes `entity.currentAmount()` correctly.

## Goals / Non-Goals

**Goals:**
- `currentAmount` supplied at creation is persisted as-is
- If `currentAmount` is omitted (null), default it to `initialAmount` (existing behaviour preserved)

**Non-Goals:**
- Validation that `currentAmount ≥ 0` or any other business-rule enforcement — out of scope
- Changing the `PUT /accounts/{id}` update path — already handles `currentAmount` independently

## Decisions

**Fix location: `AccountMapper.toEntity(CreateAccountRequest)`**

The mapper is the single source of truth for translating the DTO to an entity. Fixing it here means no changes are needed in the service or repository.

Alternative — fix in the service: rejected because mapping logic belongs in the mapper, not scattered across the service layer.

**Default behaviour: `currentAmount` falls back to `initialAmount` when null**

Keeps backward compatibility for all existing clients that send only `initialAmount`.

## Risks / Trade-offs

- [Existing tests] Tests asserting `currentAmount == initialAmount` at creation will fail if they send a different `currentAmount`. → Update those tests to reflect correct new behaviour.
- No risk of data migration — no stored data changes; only the insertion path is affected.

## Migration Plan

No schema changes. Deploy normally. No rollback needed beyond reverting the mapper commit.
