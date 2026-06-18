## Context

`user_id` on `GET /expenses` is implemented as optional with default-to-caller (`ExpenseController.list` uses `required = false`; `ExpenseService.listByUser` resolves a null/blank value to `currentUser.requireSubject()` and 404s a non-matching supplied user). The `owner-scoped-authorization` spec documents this correctly, but the older `list-expenses-by-user` and `expense-search-filters` specs still describe `user_id` as required (400 on absence). This is documentation drift only.

## Goals / Non-Goals

**Goals:**
- Make the two stale specs agree with the implemented behavior and with `owner-scoped-authorization`.

**Non-Goals:**
- No change to runtime behavior, controllers, services, or tests.
- No change to the `owner-scoped-authorization` spec (already correct) — the corrected specs cross-reference it for the matching/404 rule rather than duplicating it.

## Decisions

- **Reconcile specs to code, not code to specs.** The optional/default-to-caller behavior is the intended design (identity inferred from the token); making `user_id` required again would regress it. Confirmed direction: specs follow code.
- **Single source for the matching rule.** The 404-on-non-matching-user behavior stays defined in `owner-scoped-authorization`; `list-expenses-by-user` references it instead of restating it, to avoid re-introducing contradictions later.

## Risks / Trade-offs

- [Readers who relied on the old "required → 400" contract] → The corrected text and the `owner-scoped-authorization` spec make the real behavior explicit; no client actually received 400 for a missing `user_id`, so this only fixes documentation.

## Migration Plan

Spec-only; applied by archiving this change. No deployment or rollback considerations.
