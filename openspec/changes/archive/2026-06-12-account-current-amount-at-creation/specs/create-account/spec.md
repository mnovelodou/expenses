## ADDED Requirements

### Requirement: currentAmount is respected at account creation
When a client supplies `currentAmount` in the `POST /accounts` request body, the system SHALL persist that value as the account's current balance. If `currentAmount` is omitted or null, the system SHALL default it to `initialAmount`.

#### Scenario: Client provides a currentAmount different from initialAmount
- **WHEN** a `POST /accounts` request includes `value.initialAmount = 100` and `value.currentAmount = 250`
- **THEN** the created account has `initialAmount = 100` and `currentAmount = 250`

#### Scenario: Client omits currentAmount
- **WHEN** a `POST /accounts` request includes `value.initialAmount = 100` and no `currentAmount`
- **THEN** the created account has `initialAmount = 100` and `currentAmount = 100`

#### Scenario: Client provides currentAmount of zero
- **WHEN** a `POST /accounts` request includes `value.initialAmount = 500` and `value.currentAmount = 0`
- **THEN** the created account has `initialAmount = 500` and `currentAmount = 0`
