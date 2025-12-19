# ADVANCED SEARCH & FILTERING - API DOCUMENTATION
## Complete REST API Reference

**Last Updated:** 16 December 2025  
**Version:** 1.0  
**Base URL:** `http://localhost:8080/api/transactions`

---

## TABLE OF CONTENTS

1. [Main Search Endpoint](#main-search-endpoint)
2. [Specialized Search Endpoints](#specialized-search-endpoints)
3. [Export Endpoints](#export-endpoints)
4. [Statistics Endpoints](#statistics-endpoints)
5. [Query Examples](#query-examples)
6. [Response Formats](#response-formats)
7. [Error Handling](#error-handling)

---

## MAIN SEARCH ENDPOINT

### 1. Advanced Search with Multiple Filters

**Endpoint:** `GET /search`

**Method:** GET

**Full URL:** `http://localhost:8080/api/transactions/search`

#### Query Parameters:

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `senderWallet` | String | No | Sender wallet number (exact match) | `WAL001` |
| `receiverWallet` | String | No | Receiver wallet number (exact match) | `WAL002` |
| `status` | Enum | No | Transaction status | `COMPLETED`, `PENDING`, `FAILED` |
| `type` | Enum | No | Transaction type | `TRANSFER`, `PAYMENT`, `TOPUP` |
| `amountMin` | BigDecimal | No | Minimum amount (inclusive) | `100.00` |
| `amountMax` | BigDecimal | No | Maximum amount (inclusive) | `10000.00` |
| `fromDate` | DateTime | No | Start date (inclusive, ISO format) | `2025-01-01T00:00:00` |
| `toDate` | DateTime | No | End date (inclusive, ISO format) | `2025-12-31T23:59:59` |
| `reference` | String | No | Transaction reference (partial match, case-insensitive) | `TXN`, `TXN001` |
| `currency` | String | No | Currency code | `USD`, `EUR`, `GBP` |
| `route` | String | No | Transaction route | `WALLET_INTERNAL`, `BANK_GATEWAY_X` |
| `scheduled` | Boolean | No | Scheduled transaction flag | `true`, `false` |
| `page` | Integer | No | Page number (0-indexed, default: 0) | `0`, `1`, `5` |
| `size` | Integer | No | Page size (default: 20, max: 1000) | `10`, `20`, `50`, `100` |
| `sortBy` | String | No | Sort field (default: createdAt) | `createdAt`, `amount`, `status` |
| `direction` | Enum | No | Sort direction (default: DESC) | `ASC`, `DESC` |

#### Example Requests:

**1. Basic Search - All Completed Transactions:**
```bash
curl -X GET "http://localhost:8080/api/transactions/search?status=COMPLETED" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**2. Search with Amount Filter:**
```bash
curl -X GET "http://localhost:8080/api/transactions/search?amountMin=100&amountMax=1000&sortBy=amount&direction=ASC" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**3. Search with Date Range:**
```bash
curl -X GET "http://localhost:8080/api/transactions/search?fromDate=2025-12-01T00:00:00&toDate=2025-12-31T23:59:59&page=0&size=50" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**4. Complex Multi-Filter Search:**
```bash
curl -X GET "http://localhost:8080/api/transactions/search?senderWallet=WAL001&status=COMPLETED&amountMin=500&currency=USD&page=0&size=20&sortBy=createdAt&direction=DESC" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**5. Search by Reference (Partial Match):**
```bash
curl -X GET "http://localhost:8080/api/transactions/search?reference=TXN&page=0&size=100" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Success Response (200 OK):

```json
{
  "content": [
    {
      "id": 1,
      "reference": "550e8400-e29b-41d4-a716-446655440000",
      "senderWalletNumber": "WAL001",
      "receiverWalletNumber": "WAL002",
      "amount": 500.00,
      "currency": "USD",
      "feeAmount": 2.50,
      "feeCurrency": "USD",
      "feeType": "FIXED",
      "route": "WALLET_INTERNAL",
      "type": "TRANSFER",
      "status": "COMPLETED",
      "channel": "MOBILE",
      "description": "Payment for services",
      "authorizationMethod": "PIN",
      "authorizationRequired": true,
      "retryCount": 0,
      "maxRetry": 3,
      "scheduled": false,
      "metadata": "{}",
      "initiatedBy": "USER123",
      "lastUpdatedBy": "SYSTEM",
      "riskScore": 0,
      "complianceStatus": "CLEAR",
      "createdAt": "2025-12-16T10:30:00",
      "completedAt": "2025-12-16T10:35:00",
      "lastUpdatedAt": "2025-12-16T10:35:00"
    },
    {
      "id": 2,
      "reference": "660e8400-e29b-41d4-a716-446655440001",
      "senderWalletNumber": "WAL003",
      "receiverWalletNumber": "WAL004",
      "amount": 1000.00,
      "currency": "EUR",
      "feeAmount": 5.00,
      "feeCurrency": "EUR",
      "feeType": "FIXED",
      "route": "BANK_GATEWAY_X",
      "type": "PAYMENT",
      "status": "COMPLETED",
      "channel": "WEB",
      "description": "Bill payment",
      "authorizationMethod": "OTP",
      "authorizationRequired": true,
      "retryCount": 0,
      "maxRetry": 3,
      "scheduled": false,
      "metadata": "{}",
      "initiatedBy": "USER456",
      "lastUpdatedBy": "SYSTEM",
      "riskScore": 5,
      "complianceStatus": "CLEAR",
      "createdAt": "2025-12-16T11:00:00",
      "completedAt": "2025-12-16T11:05:00",
      "lastUpdatedAt": "2025-12-16T11:05:00"
    }
  ],
  "pageable": {
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 20,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalElements": 2,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

---

## SPECIALIZED SEARCH ENDPOINTS

### 2. Search by Sender Wallet

**Endpoint:** `GET /search/sender/{walletNumber}`

**Method:** GET

**Full URL:** `http://localhost:8080/api/transactions/search/sender/WAL001`

#### Path Parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `walletNumber` | String | Yes | Sender wallet number |

#### Query Parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `page` | Integer | No | Page number (default: 0) |
| `size` | Integer | No | Page size (default: 20, max: 1000) |
| `sortBy` | String | No | Sort field (default: createdAt) |
| `direction` | Enum | No | Sort direction (default: DESC) |

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/transactions/search/sender/WAL001?page=0&size=20&sortBy=createdAt&direction=DESC" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 3. Search by Receiver Wallet

**Endpoint:** `GET /search/receiver/{walletNumber}`

**Method:** GET

**Full URL:** `http://localhost:8080/api/transactions/search/receiver/WAL002`

#### Path Parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `walletNumber` | String | Yes | Receiver wallet number |

#### Query Parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `page` | Integer | No | Page number (default: 0) |
| `size` | Integer | No | Page size (default: 20, max: 1000) |
| `sortBy` | String | No | Sort field (default: createdAt) |
| `direction` | Enum | No | Sort direction (default: DESC) |

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/transactions/search/receiver/WAL002?page=0&size=50" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 4. Search by Amount Range

**Endpoint:** `GET /search/amount`

**Method:** GET

**Full URL:** `http://localhost:8080/api/transactions/search/amount?min=100&max=1000`

#### Query Parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `min` | BigDecimal | Yes | Minimum amount |
| `max` | BigDecimal | Yes | Maximum amount |
| `page` | Integer | No | Page number (default: 0) |
| `size` | Integer | No | Page size (default: 20, max: 1000) |
| `sortBy` | String | No | Sort field (default: createdAt) |
| `direction` | Enum | No | Sort direction (default: DESC) |

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/transactions/search/amount?min=100&max=10000&page=0&size=20&sortBy=amount&direction=ASC" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 5. Search by Status

**Endpoint:** `GET /search/status/{status}`

**Method:** GET

**Full URL:** `http://localhost:8080/api/transactions/search/status/COMPLETED`

#### Path Parameters:

| Parameter | Type | Required | Description | Valid Values |
|-----------|------|----------|-------------|--------------|
| `status` | Enum | Yes | Transaction status | INITIATED, PENDING, AUTHORIZED, PROCESSING, COMPLETED, FAILED, CANCELLED, REVERSED, REFUNDED, EXPIRED, ON_HOLD, UNDER_REVIEW |

#### Query Parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `from` | DateTime | No | Start date (ISO format) |
| `to` | DateTime | No | End date (ISO format) |
| `page` | Integer | No | Page number (default: 0) |
| `size` | Integer | No | Page size (default: 20, max: 1000) |
| `sortBy` | String | No | Sort field (default: createdAt) |
| `direction` | Enum | No | Sort direction (default: DESC) |

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/transactions/search/status/COMPLETED?from=2025-12-01T00:00:00&to=2025-12-31T23:59:59&page=0&size=100" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 6. Search by Type

**Endpoint:** `GET /search/type/{type}`

**Method:** GET

**Full URL:** `http://localhost:8080/api/transactions/search/type/TRANSFER`

#### Path Parameters:

| Parameter | Type | Required | Description | Valid Values |
|-----------|------|----------|-------------|--------------|
| `type` | Enum | Yes | Transaction type | TRANSFER, PAYMENT, TOPUP, WITHDRAWAL, SCHEDULED |

#### Query Parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `currency` | String | No | Currency code |
| `page` | Integer | No | Page number (default: 0) |
| `size` | Integer | No | Page size (default: 20, max: 1000) |
| `sortBy` | String | No | Sort field (default: createdAt) |
| `direction` | Enum | No | Sort direction (default: DESC) |

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/transactions/search/type/TRANSFER?currency=USD&page=0&size=50" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## EXPORT ENDPOINTS

### 7. Export Transactions

**Endpoint:** `GET /export`

**Method:** GET

**Full URL:** `http://localhost:8080/api/transactions/export`

#### Query Parameters:

| Parameter | Type | Required | Description | Valid Values |
|-----------|------|----------|-------------|--------------|
| `format` | String | No | Export format (default: CSV) | `CSV`, `JSON` |
| `senderWallet` | String | No | Filter by sender wallet | |
| `receiverWallet` | String | No | Filter by receiver wallet | |
| `status` | Enum | No | Filter by status | COMPLETED, PENDING, etc. |
| `type` | Enum | No | Filter by type | TRANSFER, PAYMENT, etc. |
| `amountMin` | BigDecimal | No | Minimum amount | |
| `amountMax` | BigDecimal | No | Maximum amount | |
| `fromDate` | DateTime | No | Start date | ISO format |
| `toDate` | DateTime | No | End date | ISO format |
| `currency` | String | No | Currency code | USD, EUR, etc. |
| `page` | Integer | No | Page number (default: 0) | |
| `size` | Integer | No | Page size (default: 1000, max: 10000) | |

#### Example Requests:

**1. Export Completed Transactions as CSV:**
```bash
curl -X GET "http://localhost:8080/api/transactions/export?format=CSV&status=COMPLETED" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o transactions.csv
```

**2. Export Transactions by Date Range as JSON:**
```bash
curl -X GET "http://localhost:8080/api/transactions/export?format=JSON&fromDate=2025-12-01T00:00:00&toDate=2025-12-31T23:59:59" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o transactions.json
```

**3. Export High-Value Transactions:**
```bash
curl -X GET "http://localhost:8080/api/transactions/export?format=CSV&amountMin=5000&currency=USD&page=0&size=5000" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o high_value_transactions.csv
```

#### CSV Response Format:
```
Content-Type: text/csv
Content-Disposition: attachment; filename="transactions_1702829400000.csv"

ID,Reference,Sender Wallet,Receiver Wallet,Amount,Currency,Fee,Status,Type,Route,Created At,Completed At,Risk Score
1,"550e8400-e29b-41d4-a716-446655440000","WAL001","WAL002",500.00,USD,2.50,COMPLETED,TRANSFER,WALLET_INTERNAL,2025-12-16T10:30:00,2025-12-16T10:35:00,0
2,"660e8400-e29b-41d4-a716-446655440001","WAL003","WAL004",1000.00,EUR,5.00,COMPLETED,PAYMENT,BANK_GATEWAY_X,2025-12-16T11:00:00,2025-12-16T11:05:00,5
```

#### JSON Response Format:
```json
[
  {
    "id": 1,
    "reference": "550e8400-e29b-41d4-a716-446655440000",
    "senderWalletNumber": "WAL001",
    "receiverWalletNumber": "WAL002",
    "amount": 500.00,
    "currency": "USD",
    "feeAmount": 2.50,
    "feeCurrency": "USD",
    "type": "TRANSFER",
    "status": "COMPLETED",
    "route": "WALLET_INTERNAL",
    "description": "Payment for services",
    "riskScore": 0,
    "createdAt": "2025-12-16T10:30:00",
    "completedAt": "2025-12-16T10:35:00",
    "failedAt": null,
    "cancelledAt": null
  }
]
```

---

## STATISTICS ENDPOINTS

### 8. Get Status Statistics

**Endpoint:** `GET /statistics/status`

**Method:** GET

**Full URL:** `http://localhost:8080/api/transactions/statistics/status`

#### Query Parameters:
None

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/transactions/statistics/status" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Success Response (200 OK):
```json
{
  "COMPLETED": 1234,
  "PENDING": 45,
  "FAILED": 12,
  "AUTHORIZED": 89,
  "INITIATED": 5,
  "CANCELLED": 8,
  "UNDER_REVIEW": 3,
  "ON_HOLD": 2,
  "REVERSED": 1,
  "REFUNDED": 2,
  "EXPIRED": 0,
  "PROCESSING": 15
}
```

---

## QUERY EXAMPLES

### Real-World Use Cases:

#### 1. Dashboard - Today's Transactions:
```bash
curl -X GET "http://localhost:8080/api/transactions/search?fromDate=2025-12-16T00:00:00&toDate=2025-12-16T23:59:59&page=0&size=100&sortBy=createdAt&direction=DESC" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 2. Compliance - Suspicious Transactions:
```bash
curl -X GET "http://localhost:8080/api/transactions/search?amountMin=50000&page=0&size=50&sortBy=riskScore&direction=DESC" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 3. Audit - User Transaction History:
```bash
curl -X GET "http://localhost:8080/api/transactions/search/sender/WAL001?page=0&size=100&sortBy=createdAt&direction=DESC" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 4. Reporting - Monthly Transaction Export:
```bash
curl -X GET "http://localhost:8080/api/transactions/export?format=CSV&status=COMPLETED&fromDate=2025-12-01T00:00:00&toDate=2025-12-31T23:59:59&size=10000" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o december_2025_transactions.csv
```

#### 5. Reconciliation - Failed Transactions:
```bash
curl -X GET "http://localhost:8080/api/transactions/search?status=FAILED&page=0&size=100&sortBy=createdAt&direction=DESC" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 6. Analytics - Currency Distribution:
```bash
curl -X GET "http://localhost:8080/api/transactions/search?type=TRANSFER&currency=USD&page=0&size=1000" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## RESPONSE FORMATS

### Pagination Metadata:
```json
{
  "pageable": {
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 20,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalElements": 2,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

### DateTime Format:
All dates are in ISO 8601 format: `YYYY-MM-DDTHH:mm:ss`

Examples:
- `2025-12-16T10:30:00`
- `2025-01-01T00:00:00`
- `2025-12-31T23:59:59`

### Enum Values:

**TransactionStatus:**
- INITIATED, PENDING, AUTHORIZED, PROCESSING, COMPLETED, FAILED, CANCELLED, REVERSED, REFUNDED, EXPIRED, ON_HOLD, UNDER_REVIEW

**TransactionType:**
- TRANSFER, PAYMENT, TOPUP, WITHDRAWAL, SCHEDULED

**Sort Direction:**
- ASC (Ascending), DESC (Descending)

---

## ERROR HANDLING

### Common Error Responses:

#### 400 Bad Request:
```json
{
  "timestamp": "2025-12-16T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid date format. Use ISO 8601: YYYY-MM-DDTHH:mm:ss",
  "path": "/api/transactions/search"
}
```

#### 401 Unauthorized:
```json
{
  "timestamp": "2025-12-16T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid authentication token",
  "path": "/api/transactions/search"
}
```

#### 404 Not Found:
```json
{
  "timestamp": "2025-12-16T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Transaction not found with ID: 999",
  "path": "/api/transactions/999"
}
```

#### 500 Internal Server Error:
```json
{
  "timestamp": "2025-12-16T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/transactions/search"
}
```

---

## RATE LIMITING & PERFORMANCE

- **Max Results per Page:** 1000 (hardcoded limit)
- **Default Page Size:** 20
- **Recommended Page Size:** 20-100 (optimal performance)
- **Expected Response Time:**
  - Simple queries: <50ms
  - Complex queries with 100K+ records: <500ms
  - Export 10K records CSV: <1s
  - Export 10K records JSON: <1.5s

---

**Last Updated:** 16 December 2025  
**API Version:** 1.0  
**Status:** Production Ready ✅
