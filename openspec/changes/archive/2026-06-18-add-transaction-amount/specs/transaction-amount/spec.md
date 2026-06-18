## ADDED Requirements

### Requirement: Expense records an optional original transaction amount
An expense SHALL carry an optional `transactionAmount` field (decimal) representing the amount of the original real-world transaction it was derived from. The value SHALL be supplied by the API caller only; the system SHALL NOT derive `transactionAmount` from `amount` changes or split logic. When the caller omits `transactionAmount` on **create** (`POST /expenses`) or **bulk create** (`POST /expenses/bulk`), the system SHALL default it to that line's `amount` before persisting, and the resolved value SHALL be returned in the response. This defaulting SHALL NOT apply on update — see the "Update preserves omitted amount and transaction amount" requirement, under which an omitted `transactionAmount` preserves the stored value. When a single transaction is split into multiple expense lines, each line SHALL be able to carry the same explicitly-supplied `transactionAmount` equal to the original transaction total, independent of each line's own `amount`. The field SHALL be accepted on create (`POST /expenses`), bulk create (`POST /expenses/bulk`), and update (`PUT /expenses/{id}`), and SHALL be returned on every expense representation.

#### Scenario: Create an expense with a transaction amount
- **WHEN** a `POST /expenses` request supplies `transactionAmount = 100.00` and `amount = 60.00`
- **THEN** the system returns HTTP 201 and the created expense has `transactionAmount = 100.00` and `amount = 60.00`

#### Scenario: Create an expense without a transaction amount — defaults to amount
- **WHEN** a `POST /expenses` request omits `transactionAmount` and supplies `amount = 60.00`
- **THEN** the system returns HTTP 201 and the created expense has `transactionAmount = 60.00`

#### Scenario: Split lines share the original transaction amount
- **WHEN** two expenses are created with `amount = 60.00` and `amount = 40.00`, both with `transactionAmount = 100.00`
- **THEN** both stored expenses have `transactionAmount = 100.00` while keeping their own distinct `amount`

#### Scenario: Update sets the transaction amount
- **WHEN** a `PUT /expenses/{id}` request supplies `transactionAmount = 100.00`
- **THEN** the system returns HTTP 200 and the updated expense has `transactionAmount = 100.00`

### Requirement: Update preserves omitted amount and transaction amount
On `PUT /expenses/{id}`, `amount` and `transactionAmount` SHALL be treated as partial: when either is omitted (null) in the request, the system SHALL preserve the currently persisted value for that field rather than rejecting the request or recomputing a default. The create-time rule that defaults a null `transactionAmount` to `amount` SHALL apply only on creation, never on update. A null `amount` on update SHALL NOT produce a validation error.

#### Scenario: Update with null amount preserves the existing amount
- **WHEN** a `PUT /expenses/{id}` request omits `amount` for an expense currently stored with `amount = 60.00`
- **THEN** the system returns HTTP 200 and the updated expense retains `amount = 60.00` (no HTTP 400)

#### Scenario: Update with null transactionAmount preserves the existing transaction amount
- **WHEN** a `PUT /expenses/{id}` request omits `transactionAmount` for an expense currently stored with `transactionAmount = 100.00`
- **THEN** the system returns HTTP 200 and the updated expense retains `transactionAmount = 100.00` (it is not re-defaulted to the line's `amount`)

#### Scenario: Update changes both amount and transaction amount explicitly
- **WHEN** a `PUT /expenses/{id}` request supplies both `amount = 70.00` and `transactionAmount = 120.00`
- **THEN** the system returns HTTP 200 and the updated expense has `amount = 70.00` and `transactionAmount = 120.00`

### Requirement: Monetary values bounded to NUMERIC(15,2)
Both `amount` and `transactionAmount` are persisted as `NUMERIC(15,2)`. On create, bulk create, and update the system SHALL reject a supplied value that does not fit this column — more than two fractional digits, or more than thirteen integer digits — with HTTP 400, rather than letting the database silently round the value or fail with a server error. A null value is not subject to this check (it is defaulted or preserved per the rules above).

#### Scenario: More than two fractional digits rejected
- **WHEN** a `POST /expenses` request supplies `transactionAmount = 100.123`
- **THEN** the system returns HTTP 400 and no expense is created

#### Scenario: More than thirteen integer digits rejected
- **WHEN** a `POST /expenses` request supplies `amount = 12345678901234` (14 integer digits)
- **THEN** the system returns HTTP 400 and no expense is created

### Requirement: Search expenses by transaction amount
The `GET /expenses` criteria search SHALL accept an optional `transaction_amount` query parameter. When supplied, results SHALL be restricted to expenses whose `transaction_amount` equals the supplied value exactly. When absent, `transaction_amount` SHALL NOT constrain results. The filter reuses the existing mandatory `created_by` + date-range criteria search and the existing forward cursor pagination; the `transaction_amount` filter SHALL NOT be encoded in the cursor and the caller SHALL supply it on every page request. `transaction_amount` MAY be combined with the `category`, `subcategory`, and `accountId` filters. This change SHALL NOT add a database index for the new column.

#### Scenario: Filter by transaction amount returns matching split lines
- **WHEN** a `GET /expenses` request includes `transaction_amount=100.00` within the required user and date window
- **THEN** the system returns HTTP 200 with only the expenses whose `transaction_amount = 100.00`, ordered by `expense_date DESC, expense_id DESC`

#### Scenario: transaction_amount absent — not constrained
- **WHEN** a `GET /expenses` request omits `transaction_amount`
- **THEN** the result set is not narrowed by transaction amount

#### Scenario: transaction_amount combined with accountId
- **WHEN** a `GET /expenses` request includes both `transaction_amount=100.00` and `account_id=3`
- **THEN** the system returns only expenses matching both predicates

#### Scenario: transaction_amount pagination preserves the filter
- **WHEN** a follow-up request includes a valid `cursor` and the same `transaction_amount` value as the first page
- **THEN** the system returns the next page of expenses still filtered to that `transaction_amount`
