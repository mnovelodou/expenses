## ADDED Requirements

### Requirement: initialAmount is mutable on update
The system SHALL allow callers to change `initialAmount` when updating an account via `PUT /accounts/{id}`. When `initialAmount` changes, the system SHALL adjust `current_amount` by the same delta (`new initialAmount − old initialAmount`) so that existing transaction history remains consistent.

#### Scenario: Update with a new initialAmount
- **WHEN** a `PUT /accounts/{id}` request is sent with a different `initialAmount` than the stored value
- **THEN** the account is persisted with the new `initialAmount`
- **THEN** `current_amount` is adjusted by `(new initialAmount − old initialAmount)`
- **THEN** the response body reflects the updated `initialAmount` and `current_amount`

#### Scenario: Update with the same initialAmount
- **WHEN** a `PUT /accounts/{id}` request is sent with the same `initialAmount` as the stored value
- **THEN** `initialAmount` and `current_amount` remain unchanged

#### Scenario: Update without providing initialAmount
- **WHEN** a `PUT /accounts/{id}` request omits `initialAmount` (null)
- **THEN** the existing `initialAmount` is preserved and `current_amount` is unchanged
