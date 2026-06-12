## 1. Fix Mapper

- [x] 1.1 In `AccountMapper.toEntity(CreateAccountRequest)`, replace the hardcoded `initialAmount` fallback for `currentAmount` with `Optional.ofNullable(account.currentAmount()).orElse(...)` defaulting to `initialAmount`

## 2. Update Tests

- [x] 2.1 Update any unit tests in `AccountMapperTest` (or equivalent) that assert `currentAmount == initialAmount` at creation when a distinct `currentAmount` was supplied
- [x] 2.2 Add unit test covering the three spec scenarios: distinct `currentAmount` provided, `currentAmount` omitted (defaults to `initialAmount`), and `currentAmount = 0`
- [x] 2.3 Add or update integration test in `AccountControllerIT` to send `currentAmount` differing from `initialAmount` at creation and assert the response reflects it
