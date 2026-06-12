# Spec: Get Expense by ID

## Purpose

Retrieve a single expense record by its numeric ID via the `GET /expenses/{id}` endpoint.

## Requirements

### Requirement: Authorization required for single expense retrieval
`GET /expenses/{id}` SHALL reject unauthenticated requests and under-scoped requests before looking up the expense. A request without a valid Bearer token SHALL return HTTP 401. A request with a valid token lacking the `read:expenses` scope SHALL return HTTP 403.

#### Scenario: No token — retrieval rejected with 401
- **WHEN** `GET /expenses/{id}` is called without an Authorization header
- **THEN** the system returns HTTP 401

#### Scenario: Token missing read:expenses — retrieval rejected with 403
- **WHEN** `GET /expenses/{id}` is called with a valid token that does not contain `read:expenses`
- **THEN** the system returns HTTP 403

### Requirement: Fetch single expense by ID
The system SHALL expose a `GET /expenses/{id}` endpoint that returns the full expense record for the given numeric ID.

#### Scenario: Expense exists
- **WHEN** a caller sends `GET /expenses/{id}` with a valid existing expense ID
- **THEN** the system returns HTTP 200 with the `Expense` JSON object

#### Scenario: Expense not found
- **WHEN** a caller sends `GET /expenses/{id}` with an ID that does not exist
- **THEN** the system returns HTTP 404 with an appropriate error message

#### Scenario: Invalid ID format
- **WHEN** a caller sends `GET /expenses/{id}` where `id` is not a valid number
- **THEN** the system returns HTTP 400
