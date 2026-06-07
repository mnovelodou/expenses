## ADDED Requirements

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
