## 1. Reconcile specs

- [x] 1.1 Apply the MODIFIED `list-expenses-by-user` requirement (user_id optional/default-to-caller; missing → 200; cross-reference owner-scoped-authorization for the 404 rule)
- [x] 1.2 Apply the MODIFIED `expense-search-filters` requirement (drop the "userId mandatory" claim; date range mandatory, user_id optional)

## 2. Verify

- [x] 2.1 Confirm no remaining spec asserts `user_id` is required or that a missing `user_id` returns 400 (`grep` the live specs)
- [x] 2.2 `openspec validate --specs --all` passes
