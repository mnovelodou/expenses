## 1. Repository

- [x] 1.1 Add `initial_amount = ?` to the `UPDATE` SQL in `AccountRepository.update` and pass `entity.initialAmount()` as the first new parameter

## 2. Service

- [x] 2.1 In `AccountService.update`, read the existing entity's `initialAmount` before calling `repo.update`
- [x] 2.2 Compute the delta (`newInitialAmount − oldInitialAmount`) and set `currentAmount = existingCurrentAmount + delta` on the entity passed to `repo.update`
- [x] 2.3 Preserve existing `initialAmount` when the request provides `null` for that field

## 3. Unit Tests

- [x] 3.1 In `AccountServiceTest`: add test — update with a changed `initialAmount` adjusts `current_amount` by delta
- [x] 3.2 In `AccountServiceTest`: add test — update with same `initialAmount` leaves `current_amount` unchanged
- [x] 3.3 In `AccountServiceTest`: add test — update with null `initialAmount` preserves existing value and `current_amount`
- [x] 3.4 In `AccountRepositoryTest`: add test — `update` persists a new `initial_amount` value

## 4. Integration Tests

- [x] 4.1 In `AccountControllerIT`: add test — `PUT /accounts/{id}` with a new `initialAmount` returns updated `initialAmount` and adjusted `currentAmount`
- [x] 4.2 In `AccountControllerIT`: add test — `PUT /accounts/{id}` with null `initialAmount` leaves `initialAmount` and `currentAmount` unchanged
