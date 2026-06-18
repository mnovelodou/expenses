## Why

When a single real-world transaction is split into multiple expense lines (e.g. a $100 store purchase broken into Groceries $60 and Household $40), the original transaction total is lost — each split line only records its own `amount`. Users need to preserve the original transaction total on every split line so they can later locate all expenses that originated from the same transaction by searching against that total.

## What Changes

- Add a new optional `transactionAmount` field to the `Expense` DTO and a `transaction_amount` column to the `expenses` table. It records the amount of the original (pre-split) transaction. It is supplied by the API caller only — the system never derives it from `amount` changes or split logic. On **create/bulk-create**, when the caller omits it the system defaults it to the line's `amount`.
- Persist `transactionAmount` on create, bulk-create, and update so split lines can carry the original total.
- Make `PUT /expenses/{id}` partial for `amount` and `transactionAmount`: when either is omitted, the system preserves the currently stored value instead of returning a validation error. The create-time default-to-`amount` does not apply on update.
- Add an optional `transaction_amount` query parameter to `GET /expenses` that filters results to expenses whose `transaction_amount` matches exactly. It reuses the existing criteria search (mandatory `created_by` + date range) and the existing cursor pagination — no new index is added in this change.

## Capabilities

### New Capabilities
- `transaction-amount`: Recording the original transaction total on expense lines and searching expenses by that total via the existing cursor-paginated criteria search.

### Modified Capabilities
- `list-expenses-by-user`: The `GET /expenses` criteria search gains an additional optional `transaction_amount` filter alongside the existing required `created_by` + date range and optional category/subcategory/account filters.
- `bulk-create-expenses`: Bulk-created expenses MAY carry a `transactionAmount` so a transaction split into many lines preserves the original total on each line.

## Impact

- **Schema**: `expenses` table gains a nullable `transaction_amount NUMERIC(15,2)` column (additive migration; no index in this change).
- **DTO/Entity**: `Expense`, `ExpenseEntity`, and `ExpenseMapper` gain the new field.
- **API**: `GET /expenses` accepts a new optional `transaction_amount` query parameter; create/bulk/update request bodies accept the field on the `Expense` value.
- **Code**: `ExpenseRepository.findByFiltersCursor`, `ExpenseService.listForOwner`/`listByUser`/`listByAccount`, and `ExpenseController.list` thread the new optional filter through.
- No breaking changes: the field and filter are optional; existing clients are unaffected.
