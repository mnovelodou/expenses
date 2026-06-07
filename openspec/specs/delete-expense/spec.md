# Spec: Delete Expense

## Purpose

Permanently remove an expense record by its numeric ID via the `DELETE /expenses/{id}` endpoint.

## Requirements

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
