# expenses
This repo contains an spring boot application that includes APIs for tracking expenses

## To run locally
Make sure you have docker installed:
- ./gradlew localRun

## To stop postgres
- docker-compose down -v

## Curl examples

Create
```bash
curl -i -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "value": { 
        "name": "Bank of America",
        "accountType": "CREDIT",
        "currency": "USD",
        "initialAmount": 1000.00,
        "createdBy": "user-1"
    }
  }'
```

Update
```bash
curl -i -X PUT http://localhost:8080/accounts/4 \
  -H "Content-Type: application/json" \
  -d '{ 
        "value": {
            "name": "Checking Updated",
            "accountType": "CREDIT",
            "currency": "USD",
            "initialAmount": 1000.00,
            "currentAmount": 1500.00,
            "createdBy": "user-1"
        }
    }'
```

Find All
```bash
curl -i -X GET http://localhost:8080/accounts
```

