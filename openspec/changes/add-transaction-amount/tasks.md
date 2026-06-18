## 1. Schema

- [x] 1.1 In `src/main/resources/db/schema.sql`, add an additive migration `ALTER TABLE expenses ADD COLUMN IF NOT EXISTS transaction_amount NUMERIC(15, 2);` (no index, per design)

## 2. Entity, DTO, and mapper

- [x] 2.1 Add `BigDecimal transactionAmount` to `ExpenseEntity` (record component, builder field, builder setter, `toBuilder`, constructor)
- [x] 2.2 Add `BigDecimal transactionAmount` to the `Expense` DTO (record component, builder field, builder setter, `toBuilder`, constructor)
- [x] 2.3 Update `ExpenseMapper.toEntity` and `toDto` to map `transactionAmount` both directions
- [x] 2.4 Update `ExpenseRepository.MAPPER` to read `transaction_amount` via `rs.getBigDecimal("transaction_amount")`

## 3. Persistence (create / bulk / update)

- [x] 3.1 In `ExpenseService` create and bulkCreate: when the request `transactionAmount` is null, substitute the line's `amount` before persisting (creation-only default; never derived from splits). Resolved value must flow into the response DTO
- [x] 3.1a In `ExpenseService.update`: make `amount` and `transactionAmount` partial. Stop rejecting a null `amount` for the update path, and after loading the existing (owned) entity, back-fill null `amount`/`transactionAmount` from the persisted row before the SQL `UPDATE` (do NOT re-default `transactionAmount` to `amount` on update). Keep the other write validations (e.g. ownership, account existence) intact
- [x] 3.2 Update `ExpenseRepository.create` INSERT column list, placeholders, and params to include `transaction_amount`
- [x] 3.3 Update `ExpenseRepository.bulkInsert` column list, per-row placeholders, and params to include `transaction_amount`
- [x] 3.4 Update `ExpenseRepository.update` SET clause and params to include `transaction_amount`

## 4. Search by transaction amount

- [x] 4.1 Add a `BigDecimal transactionAmount` parameter to `ExpenseRepository.findByFiltersCursor` and append `AND transaction_amount = ?` when non-null (placed with the other optional predicates, before the cursor clause)
- [x] 4.2 Thread `transactionAmount` through `ExpenseService.listForOwner`, `listByUser`, and `listByAccount`
- [x] 4.3 Add an optional `@RequestParam(value = "transaction_amount", required = false) BigDecimal transactionAmount` to `ExpenseController.list` and pass it to `listByUser`

## 5. Tests

- [x] 5.1 `ExpenseRepositoryTest`: create/bulk/update persist and read back `transaction_amount`; `findByFiltersCursor` filters by exact `transaction_amount` and combines with `account_id`
- [x] 5.2 `ExpenseServiceTest`: null `transactionAmount` defaults to `amount` on create/bulk (and is returned); supplied `transactionAmount` is preserved; on update a null `amount` preserves the stored amount (no 400) and a null `transactionAmount` preserves the stored value (not re-defaulted); explicit update changes both; split lines with the same `transactionAmount` keep distinct amounts; listing filters by `transactionAmount`; absent filter is unconstrained
- [x] 5.3 `ExpenseControllerTest`: `transaction_amount` query param is bound and forwarded; create/update round-trip the field
- [x] 5.4 `ExpenseControllerIT`: end-to-end split-and-search — create multiple lines sharing a `transactionAmount`, then `GET /expenses?transaction_amount=...` returns exactly those lines with cursor pagination intact

## 6. Verify

- [x] 6.1 Run `./gradlew test integrationTest` and confirm the suite passes
