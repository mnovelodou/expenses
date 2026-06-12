# Spec: Delete Expense

## Purpose

Permanently remove an expense record by its numeric ID via the `DELETE /expenses/{id}` endpoint.

## Requirements

### Requirement: Authorization required for expense deletion
`DELETE /expenses/{id}` SHALL reject unauthenticated requests and under-scoped requests before any deletion is attempted. A request without a valid Bearer token SHALL return HTTP 401. A request with a valid token lacking the `write:expenses` scope SHALL return HTTP 403.

#### Scenario: No token — deletion rejected with 401
- **WHEN** `DELETE /expenses/{id}` is called without an Authorization header
- **THEN** the system returns HTTP 401 and no expense is deleted

#### Scenario: Token missing write:expenses — deletion rejected with 403
- **WHEN** `DELETE /expenses/{id}` is called with a valid token that does not contain `write:expenses`
- **THEN** the system returns HTTP 403 and no expense is deleted

### Requirement: Delete expense by ID
The system SHALL expose a `DELETE /expenses/{id}` endpoint that permanently removes the expense with the given numeric ID.

#### Scenario: Expense deleted successfully
- **WHEN** a caller sends `DELETE /expenses/{id}` with a valid existing expense ID
- **THEN** the system deletes the expense and returns HTTP 204 with no response body

#### Scenario: Expense not found
- **WHEN** a caller sends `DELETE /expenses/{id}` with an ID that does not exist
- **THEN** the system returns HTTP 404 with an appropriate error message

#### Scenario: Invalid ID format
- **WHEN** a caller sends `DELETE /expenses/{id}` where `id` is not a valid number
- **THEN** the system returns HTTP 400
