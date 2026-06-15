## Context

The API is a Spring Boot resource server validating Auth0 JWTs. `SecurityConfig` enforces only OAuth scopes via `@PreAuthorize("hasAuthority('SCOPE_...')")`. No code compares the authenticated principal to the resource's `createdBy`, so any signed-in user can access any other user's data. `createdBy` in production already stores the Auth0 `sub`, so identity alignment requires no data migration. Persistence is plain JDBC via `JdbcTemplate` (no JPA).

## Goals / Non-Goals

**Goals:**
- Restrict every read, write, and delete on expenses and accounts to the resource owner, identified by the JWT `sub`.
- Default finder endpoints to the caller's own data; reject cross-user requests.
- Hide non-owned resources behind 404 responses (no existence disclosure).
- Keep the change surgical: a single identity helper plus per-endpoint ownership checks.

**Non-Goals:**
- An `ADMIN` role able to act across users (deferred to a future change).
- Changing the OAuth scope model or `SecurityConfig` filter chain.
- Any database schema or data migration.
- Field-level redaction or rate limiting.

## Decisions

**Identity source — JWT `sub`.** A small helper in the `security` package resolves the current caller's `sub` from `SecurityContextHolder` (`Jwt.getSubject()`). It throws/denies if absent. All ownership checks route through this single method so the rule is defined once. Services receive the caller `sub` as an explicit argument (passed from controllers) rather than reaching into the security context themselves, keeping services unit-testable without a security context.

**Finders default to self.** `user_id` / `userId` become optional. Resolution: `requested = (param == null) ? callerSub : param`; then `if (!requested.equals(callerSub)) deny404`. This satisfies "default to self OR must equal self" and leaves a clean seam for the future ADMIN role (which will relax the equality check).

**Single-resource reads / deletes — fetch then compare.** `GET`/`DELETE` by id fetch the row first and compare `createdBy` to the caller `sub`. A non-match is treated identically to a missing row → 404. `DELETE` now reads before deleting (today it deletes blind).

**Account expenses — verify before deriving.** `GET /accounts/{id}/expenses` fetches the account, verifies `account.createdBy == callerSub`, and only then derives the owner for the expense query. This closes the current leak where the owner is taken from the account and trusted.

**Writes — two-sided check.** `body.createdBy` must equal the caller `sub` (no impersonation), and for updates the stored row's `createdBy` must also equal the caller `sub` (can't touch others' rows). With both sides pinned to the token, the existing `body.createdBy == oldExpense.createdBy` check in `ExpenseService.update` becomes redundant and is removed.

**Denial response — 404.** Non-owner access to a specific resource returns 404 Not Found to avoid revealing existence and prevent enumeration. Reuse the existing not-found exceptions (`createExpenseNotFoundException`, `createAccountNotFoundException`) so the response shape is unchanged. Impersonation on create (where there is no target row) is a client error in the request body and may surface as a 403/422-style validation/unauthorized exception consistent with the existing `createUnauthorized*` exceptions.

**Test alignment.** `BaseIT.jwt()` must set the JWT subject; integration tests must seed `createdBy` equal to that subject. Add positive (owner) and negative (non-owner → 404) cases per endpoint.

## Risks / Trade-offs

- **404-for-forbidden** trades debuggability for non-disclosure; acceptable for an owner-scoped API and consistent with the chosen requirement.
- **Services gaining a `callerSub` parameter** touches method signatures and their call sites/tests; mechanical but broad.
- **Test churn**: most integration tests change because the mock JWT subject must now line up with seeded data. Expected and necessary.
- **Create-time impersonation response code** is the one spot where the exact status (403 vs 422) is a judgement call; chosen to mirror existing `createUnauthorized*` behavior rather than 404, since the resource has no prior owner to hide.
- **ADMIN deferral**: the equality checks are written so a future role check can wrap them without restructuring.
