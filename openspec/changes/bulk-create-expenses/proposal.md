## Why

Creating expenses one at a time is inefficient for importing historical data or syncing from a bank feed. A bulk endpoint reduces round trips and enables batch workflows common in expense management tools.

## What Changes

- New `POST /expenses/bulk` endpoint that accepts an array of expense objects and creates them in a single atomic transaction.
- Returns a list of created expenses (with assigned IDs) or a validation error if any item fails.

## Capabilities

### New Capabilities
- `bulk-create-expenses`: Accept an array of expense inputs and create all of them atomically, returning the created expense objects.

### Modified Capabilities

## Impact

- New route and handler in the expenses controller
- New service method for batch insert (using JDBC batch operations or a loop within a transaction)
- No schema changes required — reuses the existing `expenses` table
- OpenAPI spec updated with new endpoint
