## ADDED Requirements

### Requirement: Bulk create preserves transaction amount on split lines
Each expense object in a `POST /expenses/bulk` request MAY include an optional `transactionAmount`. When a single transaction is split into many lines submitted in one bulk request, every line MAY carry the same `transactionAmount` equal to the original transaction total while keeping its own distinct `amount`. The field is optional per item; when omitted on an item it SHALL be defaulted to that item's own `amount`. Persistence of `transactionAmount` SHALL be atomic with the rest of the bulk insert.

#### Scenario: Bulk create splits one transaction into many lines
- **WHEN** a `POST /expenses/bulk` request contains two expenses with `amount = 60.00` and `amount = 40.00`, both with `transactionAmount = 100.00`
- **THEN** the system returns HTTP 201 and both created expenses have `transactionAmount = 100.00`

#### Scenario: Mixed items — omitted transaction amount defaults to the item's amount
- **WHEN** a `POST /expenses/bulk` request contains one expense with `amount = 60.00` and `transactionAmount = 100.00`, and one expense with `amount = 25.00` that omits `transactionAmount`
- **THEN** the system returns HTTP 201, the first created expense has `transactionAmount = 100.00`, and the second has `transactionAmount = 25.00`
