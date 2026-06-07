# Spec: Bulk Create Expenses

## Purpose

Allow clients to create multiple expenses in a single atomic request via `POST /expenses/bulk`.

## Requirements

### Requirement: Bulk create expenses
The system SHALL expose a `POST /expenses/bulk` endpoint that accepts a JSON body with an `expenses` array and creates all provided expenses atomically within a single transaction. The endpoint SHALL return HTTP 201 with a JSON body containing a `expenses` array of the created expense objects (including server-assigned IDs) on success. If any expense fails validation, the entire request SHALL be rejected with HTTP 400 and no expenses SHALL be persisted.

#### Scenario: Successful bulk creation
- **WHEN** a POST request is made to `/expenses/bulk` with a valid `expenses` array
- **THEN** the system returns HTTP 201 with a body containing a `expenses` array of created expense objects, each with a server-assigned ID

#### Scenario: Empty expenses array — rejected
- **WHEN** a POST request is made to `/expenses/bulk` with `"expenses": []`
- **THEN** the system returns HTTP 400

#### Scenario: One item fails validation — entire request rejected
- **WHEN** a POST request includes at least one expense with a missing required field (e.g., no `amount`)
- **THEN** the system returns HTTP 400 and no expenses are created

#### Scenario: Exceeds maximum batch size — rejected
- **WHEN** a POST request contains more than 200 expenses
- **THEN** the system returns HTTP 400

### Requirement: Bulk create is atomic
All expenses in a `POST /expenses/bulk` request SHALL be inserted within a single database transaction. Either all are committed or none are.

#### Scenario: Database error mid-batch rolls back all inserts
- **WHEN** a POST request to `/expenses/bulk` encounters a database error while inserting one of the expenses
- **THEN** the system returns HTTP 500, no expenses from the batch are persisted, and the transaction is rolled back

### Requirement: Individual expense validation in bulk request
Each expense object in the `expenses` array SHALL be validated against the same rules as a single `POST /expenses` request (required fields, valid types, valid accountId reference).

#### Scenario: Non-existent accountId in one item
- **WHEN** a POST request to `/expenses/bulk` contains an expense referencing a non-existent `accountId`
- **THEN** the system returns HTTP 404 and no expenses are created
