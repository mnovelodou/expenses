## ADDED Requirements

### Requirement: Caller identity is the JWT subject

The system SHALL treat the `sub` claim of the authenticated JWT as the canonical identity of the caller for all ownership decisions. The stored `createdBy` field of a resource SHALL be compared against this `sub` to determine ownership.

#### Scenario: Identity resolved from token

- **WHEN** an authenticated request is processed
- **THEN** the system resolves the caller's identity from the JWT `sub` claim in the security context

#### Scenario: Missing subject is rejected

- **WHEN** a request carries a token with no `sub` claim
- **THEN** the system denies the request rather than treating it as an anonymous or wildcard owner

### Requirement: Finders are scoped to the caller

Endpoints that list resources by user (`GET /expenses`, `GET /accounts/user/{userId}`) SHALL treat the requested user as optional and default it to the caller's `sub`. When a requested user is supplied it MUST equal the caller's `sub`.

#### Scenario: Requested user omitted defaults to caller

- **WHEN** a caller lists expenses without supplying `user_id`
- **THEN** the system returns only resources owned by the caller's `sub`

#### Scenario: Requested user matches caller

- **WHEN** a caller lists resources supplying a user equal to their own `sub`
- **THEN** the system returns that user's resources

#### Scenario: Requested user differs from caller

- **WHEN** a caller supplies a requested user that is not their own `sub`
- **THEN** the system denies the request with 404 Not Found and returns no data

### Requirement: Single-resource reads require ownership

Endpoints returning a single resource by id (`GET /expenses/{id}`, `GET /accounts/{id}`) SHALL fetch the resource and return it only when its `createdBy` equals the caller's `sub`.

#### Scenario: Owner reads own resource

- **WHEN** a caller requests a resource by id whose `createdBy` equals their `sub`
- **THEN** the system returns the resource

#### Scenario: Non-owner read is hidden

- **WHEN** a caller requests a resource by id whose `createdBy` differs from their `sub`
- **THEN** the system responds 404 Not Found without revealing the resource

#### Scenario: Missing resource

- **WHEN** a caller requests a resource id that does not exist
- **THEN** the system responds 404 Not Found

### Requirement: Account expenses require account ownership

`GET /accounts/{id}/expenses` SHALL fetch the account and return its expenses only when `account.createdBy` equals the caller's `sub`. The owner used to query expenses SHALL be derived only after ownership is verified.

#### Scenario: Owner lists account expenses

- **WHEN** a caller lists expenses for an account whose `createdBy` equals their `sub`
- **THEN** the system returns that account's expenses

#### Scenario: Non-owner cannot list account expenses

- **WHEN** a caller lists expenses for an account whose `createdBy` differs from their `sub`
- **THEN** the system responds 404 Not Found without revealing the account or its expenses

### Requirement: Writes require caller ownership

Create and update operations (`POST`/`PUT` for expenses and accounts) SHALL require `body.createdBy` to equal the caller's `sub`. Update operations SHALL additionally require the stored resource's `createdBy` to equal the caller's `sub`.

#### Scenario: Create with own identity

- **WHEN** a caller creates a resource with `body.createdBy` equal to their `sub`
- **THEN** the system creates the resource

#### Scenario: Create impersonating another user

- **WHEN** a caller submits a create request with `body.createdBy` not equal to their `sub`
- **THEN** the system denies the request and does not create the resource

#### Scenario: Update own resource

- **WHEN** a caller updates a resource where both `body.createdBy` and the stored `createdBy` equal their `sub`
- **THEN** the system applies the update

#### Scenario: Update another user's resource

- **WHEN** a caller updates a resource whose stored `createdBy` differs from their `sub`
- **THEN** the system responds 404 Not Found and does not modify the resource

#### Scenario: Update reassigning ownership

- **WHEN** a caller submits an update with `body.createdBy` not equal to their `sub`
- **THEN** the system denies the request and does not modify the resource

#### Scenario: Expense write referencing a non-owned account

- **WHEN** a caller creates or updates an expense referencing an account whose `createdBy` is not their `sub`, or an account that does not exist
- **THEN** the system responds 404 Not Found without distinguishing a non-owned account from a missing one

### Requirement: Deletes require caller ownership

Delete operations (`DELETE /expenses/{id}`, `DELETE /accounts/{id}`) SHALL fetch the resource and delete it only when its `createdBy` equals the caller's `sub`.

#### Scenario: Owner deletes own resource

- **WHEN** a caller deletes a resource whose `createdBy` equals their `sub`
- **THEN** the system deletes the resource

#### Scenario: Non-owner delete is hidden

- **WHEN** a caller deletes a resource whose `createdBy` differs from their `sub`
- **THEN** the system responds 404 Not Found and does not delete the resource
