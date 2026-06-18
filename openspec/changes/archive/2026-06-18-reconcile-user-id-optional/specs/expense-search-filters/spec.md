## MODIFIED Requirements

### Requirement: Optional filters on cursor-paginated expense list
The `GET /expenses` endpoint SHALL accept three optional query parameters: `category` (string), `subcategory` (string), and `accountId` (integer). Since a subcategory belongs to exactly one category, `category` and `subcategory` are mutually exclusive — supplying both SHALL be rejected with HTTP 400. `accountId` MAY be combined with either `category` or `subcategory`. When a filter is absent, it SHALL NOT constrain the results. The date range remains mandatory as defined in the `list-expenses-by-user` spec; `user_id` is optional and defaults to the authenticated caller.

#### Scenario: No optional filters — all expenses returned
- **WHEN** a GET request is made with only `user_id` and date range
- **THEN** the system returns HTTP 200 with all expenses for that user and date window

#### Scenario: Filter by category only
- **WHEN** a GET request includes `category=Food`
- **THEN** the system returns only expenses where `category = 'Food'` for that user and date window

#### Scenario: Filter by subcategory only
- **WHEN** a GET request includes `subcategory=Groceries`
- **THEN** the system returns only expenses where `subcategory = 'Groceries'` for that user and date window

#### Scenario: Filter by accountId only
- **WHEN** a GET request includes `accountId=3`
- **THEN** the system returns only expenses where `account_id = 3` for that user and date window

#### Scenario: Filter by subcategory and accountId combined
- **WHEN** a GET request includes both `subcategory=Groceries` and `accountId=3`
- **THEN** the system returns only expenses matching both predicates

#### Scenario: Filter by category and accountId combined
- **WHEN** a GET request includes both `category=Food` and `accountId=3`
- **THEN** the system returns only expenses matching both predicates

#### Scenario: category and subcategory supplied together — rejected
- **WHEN** a GET request includes both `category=Food` and `subcategory=Groceries`
- **THEN** the system returns HTTP 400
