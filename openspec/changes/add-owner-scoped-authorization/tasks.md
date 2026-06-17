## 1. Identity helper

- [x] 1.1 Add a helper in the `security` package that resolves the current caller's `sub` from `SecurityContextHolder` (`Jwt.getSubject()`)
- [x] 1.2 Make the helper deny/throw when no authentication or no `sub` is present (no anonymous/wildcard owner)
- [x] 1.3 Add a unit test for the helper covering present, missing-auth, and missing-sub cases

## 2. Expense reads

- [x] 2.1 `GET /expenses`: make `user_id` optional; resolve requested user as `param ?? callerSub`; deny with 404 when the supplied value differs from `callerSub`
- [x] 2.2 `GET /expenses/{id}`: fetch the expense and return 404 when `createdBy != callerSub`
- [x] 2.3 Pass `callerSub` from `ExpenseController` into `ExpenseService.listByUser` / `getById` rather than reading the security context in the service

## 3. Expense writes and deletes

- [x] 3.1 `POST /expenses` and `/bulk`: require `body.createdBy == callerSub`; reject impersonation
- [x] 3.2 `PUT /expenses/{id}`: require `body.createdBy == callerSub` and stored `createdBy == callerSub`; return 404 when the stored row is not owned
- [x] 3.3 Remove the now-redundant `body.createdBy == oldExpense.createdBy` check in `ExpenseService.update`
- [x] 3.4 `DELETE /expenses/{id}`: fetch the expense first and return 404 when `createdBy != callerSub`

## 4. Account reads

- [x] 4.1 `GET /accounts/{id}`: fetch the account and return 404 when `createdBy != callerSub`
- [x] 4.2 `GET /accounts/user/{userId}`: make the user optional/self-scoped; deny with 404 when `userId != callerSub`
- [x] 4.3 `GET /accounts/{id}/expenses`: fetch the account, verify `account.createdBy == callerSub` before deriving the owner; return 404 when not owned

## 5. Account writes and deletes

- [x] 5.1 `POST /accounts`: require `body.createdBy == callerSub`; reject impersonation
- [x] 5.2 `PUT /accounts/{id}`: require `body.createdBy == callerSub` and stored `createdBy == callerSub`; return 404 when not owned
- [x] 5.3 `DELETE /accounts/{id}`: fetch the account first and return 404 when `createdBy != callerSub`

## 6. Tests

- [x] 6.1 Update `BaseIT.jwt()` to set the JWT subject; align `createExpense`/`createAccount` helpers so seeded `createdBy` equals that subject
- [x] 6.2 Add owner-success and non-owner-404 integration cases for each expense endpoint (`SecurityIT` / `ExpenseControllerIT`)
- [x] 6.3 Add owner-success and non-owner-404 integration cases for each account endpoint (`SecurityIT` / `AccountControllerIT`)
- [x] 6.4 Add finder default-to-self cases (omitted user param returns only caller's data) for expenses and accounts
- [x] 6.5 Add impersonation-rejection cases for create and update on both expenses and accounts
- [x] 6.6 Run the full unit and integration test suite and confirm green
