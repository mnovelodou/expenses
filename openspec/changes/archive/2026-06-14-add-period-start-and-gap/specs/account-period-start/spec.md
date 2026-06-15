## ADDED Requirements

### Requirement: period_start is required on account creation
The system SHALL require `period_start` (ISO 8601 date string, e.g. `"2026-06-01"`) when creating an account. Requests missing this field SHALL be rejected with 400.

#### Scenario: Create account with period_start
- **WHEN** a POST /accounts request includes a valid `period_start` date
- **THEN** the account is created and `period_start` is returned in the response

#### Scenario: Create account without period_start
- **WHEN** a POST /accounts request omits `period_start`
- **THEN** the system returns 400 Bad Request

### Requirement: period_start is optional on account update
The system SHALL allow PATCH /accounts/{id} requests that omit `period_start`. When omitted or null, the existing `period_start` value SHALL be preserved unchanged.

#### Scenario: Update account without period_start
- **WHEN** a PATCH /accounts/{id} request omits `period_start`
- **THEN** the account is updated and the existing `period_start` is unchanged

#### Scenario: Update account with new period_start
- **WHEN** a PATCH /accounts/{id} request includes a valid `period_start` date
- **THEN** the account's `period_start` is updated to the new value

### Requirement: period_start is returned on all account reads
The system SHALL include `period_start` in the response body for GET /accounts and GET /accounts/{id}. The value MAY be null for accounts created before this feature was introduced.

#### Scenario: Read account with period_start set
- **WHEN** a GET /accounts/{id} request is made for an account with a `period_start`
- **THEN** the response includes `period_start` as an ISO 8601 date string

#### Scenario: Read account with no period_start
- **WHEN** a GET /accounts/{id} request is made for an account where `period_start` is null
- **THEN** the response includes `"period_start": null`
