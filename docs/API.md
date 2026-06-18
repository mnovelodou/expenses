# API reference

Base URL (local): `http://localhost:8080`

All endpoints are protected by an OAuth2 resource server. Requests must include a
JWT bearer token issued by your configured OAuth2 / OIDC provider:

```
Authorization: Bearer <access-token>
```

How you obtain the token depends on your provider and grant of choice — the
service only validates the token, it never issues one. See
[ARCHITECTURE.md → Authentication](ARCHITECTURE.md#authentication) for what the
provider must support and how to point the server at it. The examples below use a
`$TOKEN` shell variable:

```bash
export TOKEN="<access-token>"
```

Request bodies wrap the payload in a `value` object.

---

## Accounts

### Create an account

```bash
curl -i -X POST http://localhost:8080/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "value": {
        "name": "Bank of America",
        "accountType": "CREDIT",
        "currency": "USD",
        "initialAmount": 1000.00
    }
  }'
```

### Update an account

```bash
curl -i -X PUT http://localhost:8080/accounts/4 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "value": {
            "name": "Checking Updated",
            "accountType": "CREDIT",
            "currency": "USD",
            "initialAmount": 1000.00,
            "currentAmount": 1500.00
        }
    }'
```

### Get an account by id

```bash
curl -i http://localhost:8080/accounts/4 \
  -H "Authorization: Bearer $TOKEN"
# add ?includeGap=true to return the unexplained balance delta
```

### List the caller's accounts

```bash
curl -i http://localhost:8080/accounts/user \
  -H "Authorization: Bearer $TOKEN"
```

### Delete an account

```bash
curl -i -X DELETE http://localhost:8080/accounts/4 \
  -H "Authorization: Bearer $TOKEN"
```

### List expenses for an account

```bash
curl -i http://localhost:8080/accounts/4/expenses \
  -H "Authorization: Bearer $TOKEN"
```

---

## Expenses

### Create an expense

```bash
curl -i -X POST http://localhost:8080/expenses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "value": {
        "expenseDate": "2026-06-17",
        "accountId": 4,
        "amount": 34.56,
        "description": "Weekly groceries",
        "subCategory": "GROCERIES"
    }
  }'
```

`transactionAmount` records the original pre-split amount; when an expense is
split into multiple lines, the line `amount`s should sum back to the original
`transactionAmount`.

### Bulk create expenses

```bash
curl -i -X POST http://localhost:8080/expenses/bulk \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "value": [
        { "expenseDate": "2026-06-17", "accountId": 4, "amount": 12.20, "description": "Shirt", "subCategory": "CLOTHING", "transactionAmount": 34.56 },
        { "expenseDate": "2026-06-17", "accountId": 4, "amount": 22.36, "description": "Groceries", "subCategory": "GROCERIES", "transactionAmount": 34.56 }
    ]
  }'
```

### Get an expense by id

```bash
curl -i http://localhost:8080/expenses/10 \
  -H "Authorization: Bearer $TOKEN"
```

### List / search expenses

```bash
# cursor-paginated list for the caller, with optional criteria filters
curl -i "http://localhost:8080/expenses?limit=20" \
  -H "Authorization: Bearer $TOKEN"
```

Supported query parameters include cursor pagination and criteria search (date
window, amount, transaction amount, etc.). See the OpenSpec specs under
[`openspec/specs/expense-search-filters`](../openspec/specs/expense-search-filters)
for the authoritative list.

### Update an expense

```bash
curl -i -X PUT http://localhost:8080/expenses/10 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "value": {
        "expenseDate": "2026-06-17",
        "accountId": 4,
        "amount": 30.00,
        "description": "Groceries (corrected)",
        "subCategory": "GROCERIES"
    }
  }'
```

### Delete an expense

```bash
curl -i -X DELETE http://localhost:8080/expenses/10 \
  -H "Authorization: Bearer $TOKEN"
```
