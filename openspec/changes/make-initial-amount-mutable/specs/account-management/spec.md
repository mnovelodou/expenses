## ADDED Requirements

### Requirement: initialAmount is mutable on update
The system SHALL allow callers to change `initialAmount` when updating an account via `PUT /accounts/{id}`. `currentAmount` SHALL remain independently controlled: the caller-provided value is used when present, otherwise the stored value is preserved. No delta adjustment to `currentAmount` SHALL be derived from changes to `initialAmount`.

#### Scenario: Update with a new initialAmount
- **WHEN** a `PUT /accounts/{id}` request is sent with a new `initialAmount` and an explicit `currentAmount`
- **THEN** the account is persisted with the new `initialAmount`
- **THEN** `currentAmount` reflects the caller-provided value (not a delta)

#### Scenario: Update initialAmount without providing currentAmount
- **WHEN** a `PUT /accounts/{id}` request provides a new `initialAmount` but omits `currentAmount`
- **THEN** the account is persisted with the new `initialAmount`
- **THEN** the stored `currentAmount` is preserved unchanged

#### Scenario: Update without providing initialAmount
- **WHEN** a `PUT /accounts/{id}` request omits `initialAmount` (null)
- **THEN** the existing `initialAmount` is preserved
- **THEN** `currentAmount` follows normal update semantics (caller-provided if present, otherwise stored value)
