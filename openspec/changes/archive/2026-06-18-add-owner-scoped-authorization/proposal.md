## Why

Authorization today stops at OAuth scopes: a valid token with `read:expenses` or `write:accounts` proves only that the caller *may* perform that kind of operation in general — never that they own the specific resource. The authenticated identity (JWT `sub`) is never compared to a resource's `createdBy`. As a result any signed-in user can read, modify, and delete any other user's expenses and accounts. This is a critical broken-access-control vulnerability across the entire API surface.

## What Changes

- Introduce ownership enforcement keyed on the JWT `sub` claim as the canonical user identity. Production `createdBy` values already hold the Auth0 `sub`, so no data backfill is required.
- Add a helper to resolve the current caller's `sub` from the Spring `SecurityContext`.
- **Finders** (`GET /expenses`, `GET /accounts/user/{userId}`): the requested user becomes **optional** and defaults to the caller's `sub`; if supplied it must equal the caller's `sub`, otherwise the request is denied.
- **Single-resource reads** (`GET /expenses/{id}`, `GET /accounts/{id}`): fetch the row and deny unless `createdBy == caller sub`.
- **Account expenses** (`GET /accounts/{id}/expenses`): fetch the account and deny unless `account.createdBy == caller sub`. Today it derives `user_id` from the account and trusts it — this is the leak.
- **Writes** (`POST`/`PUT` for expenses and accounts): require `body.createdBy == caller sub` and, for updates, `stored row.createdBy == caller sub`. The now-redundant body-vs-stored ownership check in `ExpenseService.update` is removed.
- **Deletes** (`DELETE /expenses/{id}`, `DELETE /accounts/{id}`): fetch the row first and deny unless `createdBy == caller sub`. Today delete never reads the row.
- Denials for resources the caller does not own return **404 Not Found** (existence is not revealed).
- **BREAKING**: `user_id` / `userId` no longer accepted as an arbitrary value; callers can only address their own data. Requests targeting another user's data now fail with 404.
- Test harness: `BaseIT.jwt()` sets a subject and integration tests align the mock JWT `sub` with the `createdBy` they seed.

DEFERRED (not in this change): an `ADMIN` role permitted to act across users.

## Capabilities

### New Capabilities
- `owner-scoped-authorization`: Cross-cutting requirement that every read, write, and delete on expenses and accounts is restricted to the resource owner, identified by the JWT `sub`, with non-owner access returning 404.

### Modified Capabilities
<!-- Behavior is enforced via the new cross-cutting capability; existing per-endpoint specs are not changing their core requirements, only gaining the ownership precondition documented in the new capability. -->

## Impact

- **Code**: `ExpenseController`, `AccountController`, `ExpenseService`, `AccountService`; new identity helper in the `security` package.
- **APIs**: `user_id`/`userId` parameters become optional and self-scoped; non-owner access returns 404 instead of data.
- **Tests**: `BaseIT`, `SecurityIT`, `ExpenseControllerIT`, `AccountControllerIT` updated to align JWT subject with seeded `createdBy`.
- **Data**: none — `createdBy` already stores the Auth0 `sub`.
- **Stack**: plain JDBC / `JdbcTemplate`, no JPA.
