## 1. Repository

- [x] 1.1 Add `initial_amount = ?` to the `UPDATE` SQL in `AccountRepository.update` and pass `entity.initialAmount()` as the first new parameter

## 2. Service

- [x] 2.1 In `AccountService.update`, read the existing entity's `initialAmount` before calling `repo.update`
- [x] 2.2 Resolve `initialAmount` and `currentAmount` via null-coalescing: use caller-provided value if present, otherwise fall back to the stored value
- [x] 2.3 Pass the fully resolved account to `AccountMapper.toEntity` so both fields are always non-null when reaching the repository

## 3. Unit Tests

- [x] 3.1 In `AccountServiceTest`: add test — new `initialAmount` + explicit `currentAmount` persists both independently
- [x] 3.2 In `AccountServiceTest`: add test — new `initialAmount` without `currentAmount` preserves stored `currentAmount`
- [x] 3.3 In `AccountServiceTest`: add test — null `initialAmount` preserves stored `initialAmount`
- [x] 3.4 In `AccountRepositoryTest`: add test — `update` persists a new `initial_amount` value

## 4. Integration Tests

- [x] 4.1 In `AccountControllerIT`: add test — `PUT /accounts/{id}` with new `initialAmount` + explicit `currentAmount` persists both independently; also covers omitted `currentAmount` preserving stored value
- [x] 4.2 In `AccountControllerIT`: add test — `PUT /accounts/{id}` with null `initialAmount` preserves stored `initialAmount` and `currentAmount`
