# PHASE 1 - TESTING GUIDE
## Advanced Search & Filtering - Complete Test Suite

**Date:** 16 December 2025  
**Version:** 1.0  
**Scope:** All 8 endpoints, 12 filter combinations, CSV/JSON export

---

## PRE-TESTING CHECKLIST

- [ ] Database migration applied (V20251216__add_search_indexes.sql)
- [ ] TransactionService compiled without errors
- [ ] Application started successfully
- [ ] Sample test data loaded (minimum 50 transactions)
- [ ] Authentication token obtained
- [ ] Postman/cURL/REST client configured

---

## TEST DATA SETUP

### Sample Insert Query:

```sql
-- Insert 50 test transactions with various combinations
INSERT INTO transactions 
(reference, sender_wallet_number, receiver_wallet_number, amount, currency, type, status, created_at, completed_at) 
VALUES 
('TXN-001', 'WAL001', 'WAL002', 100.00, 'USD', 'TRANSFER', 'COMPLETED', NOW(), NOW()),
('TXN-002', 'WAL001', 'WAL003', 250.50, 'EUR', 'PAYMENT', 'COMPLETED', NOW(), NOW()),
('TXN-003', 'WAL002', 'WAL001', 500.00, 'USD', 'TRANSFER', 'PENDING', NOW(), NULL),
('TXN-004', 'WAL003', 'WAL002', 1000.00, 'GBP', 'TOPUP', 'COMPLETED', NOW(), NOW()),
('TXN-005', 'WAL001', 'WAL004', 75.25, 'USD', 'WITHDRAWAL', 'FAILED', NOW(), NOW()),
-- ... (repeat for 50+ transactions with various statuses, amounts, currencies)
;
```

---

## UNIT TESTS (TransactionSearchService)

### Test Class Template:

```java
@ExtendWith(MockitoExtension.class)
public class TransactionSearchServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionSearchService searchService;

    @Test
    public void testSearchWithAllFilters() {
        // GIVEN
        TransactionSearchRequest request = TransactionSearchRequest.builder()
                .senderWalletNumber("WAL001")
                .status(TransactionStatus.COMPLETED)
                .amountMin(BigDecimal.valueOf(100))
                .amountMax(BigDecimal.valueOf(1000))
                .createdFrom(LocalDateTime.of(2025, 12, 1, 0, 0))
                .createdTo(LocalDateTime.of(2025, 12, 31, 23, 59))
                .currency("USD")
                .build();
        
        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaction> expectedPage = Page.empty(pageable);
        
        when(transactionRepository.searchTransactions(
                eq("WAL001"), isNull(), eq(TransactionStatus.COMPLETED), isNull(),
                eq(BigDecimal.valueOf(100)), eq(BigDecimal.valueOf(1000)),
                any(), any(), isNull(), eq("USD"), isNull(), isNull(), eq(pageable)
        )).thenReturn(expectedPage);
        
        // WHEN
        Page<Transaction> result = searchService.search(request, pageable);
        
        // THEN
        assertNotNull(result);
        verify(transactionRepository, times(1)).searchTransactions(
                anyString(), any(), any(), any(), any(), any(), any(), any(), any(), anyString(), any(), any(), any()
        );
    }

    @Test
    public void testSearchWithNullCriteria() {
        // GIVEN
        TransactionSearchRequest request = new TransactionSearchRequest();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaction> expectedPage = Page.empty(pageable);
        
        when(transactionRepository.searchTransactions(
                isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), eq(pageable)
        )).thenReturn(expectedPage);
        
        // WHEN
        Page<Transaction> result = searchService.search(request, pageable);
        
        // THEN
        assertNotNull(result);
    }

    @Test
    public void testExportToCSV() throws IOException {
        // GIVEN
        List<Transaction> transactions = createSampleTransactions(10);
        
        // WHEN
        ByteArrayResource resource = searchService.exportToCSV(transactions);
        
        // THEN
        assertNotNull(resource);
        assertTrue(resource.contentLength() > 0);
        String content = new String(resource.getByteArray(), StandardCharsets.UTF_8);
        assertTrue(content.contains("ID,Reference,Sender Wallet"));
    }

    @Test
    public void testExportToJSON() throws IOException {
        // GIVEN
        List<Transaction> transactions = createSampleTransactions(10);
        
        // WHEN
        ByteArrayResource resource = searchService.exportToJSON(transactions);
        
        // THEN
        assertNotNull(resource);
        assertTrue(resource.contentLength() > 0);
        String content = new String(resource.getByteArray(), StandardCharsets.UTF_8);
        assertTrue(content.contains("\"id\""));
        assertTrue(content.contains("\"reference\""));
    }
    
    // Helper method
    private List<Transaction> createSampleTransactions(int count) {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Transaction tx = Transaction.builder()
                    .id((long) i)
                    .reference("TXN-" + String.format("%03d", i))
                    .senderWalletNumber("WAL001")
                    .receiverWalletNumber("WAL002")
                    .amount(BigDecimal.valueOf(100 + i * 10))
                    .currency("USD")
                    .type(TransactionType.TRANSFER)
                    .status(TransactionStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .build();
            transactions.add(tx);
        }
        return transactions;
    }
}
```

---

## INTEGRATION TESTS (TransactionController)

### Test 1: Search All Parameters ✅

**Endpoint:** `GET /api/transactions/search`

**Test Case:** Full search with all optional parameters

```bash
# Request
curl -X GET "http://localhost:8080/api/transactions/search?senderWallet=WAL001&status=COMPLETED&amountMin=100&amountMax=1000&fromDate=2025-12-01T00:00:00&toDate=2025-12-31T23:59:59&currency=USD&page=0&size=20&sortBy=createdAt&direction=DESC" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response:**
- Status: 200 OK
- Contains list of transactions matching all filters
- Pagination metadata present
- Transactions sorted by createdAt DESC

**Validation:**
```javascript
// Response should contain:
- content[].id exists
- content[].reference contains "TXN"
- content[].senderWalletNumber == "WAL001"
- content[].status == "COMPLETED"
- content[].amount between 100 and 1000
- content[].currency == "USD"
- totalElements > 0 OR == 0 (depends on test data)
- pageable.pageNumber == 0
- pageable.pageSize == 20
```

---

### Test 2: Search No Filters ✅

**Endpoint:** `GET /api/transactions/search`

**Test Case:** Search with no parameters (should return all)

```bash
curl -X GET "http://localhost:8080/api/transactions/search" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 200 OK
- All transactions returned (up to page size)
- Default pagination applied

---

### Test 3: Search by Sender ✅

**Endpoint:** `GET /api/transactions/search/sender/{walletNumber}`

**Test Case:** Search by specific sender

```bash
curl -X GET "http://localhost:8080/api/transactions/search/sender/WAL001?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 200 OK
- Only transactions where senderWalletNumber == "WAL001"
- Multiple transactions if available

---

### Test 4: Search by Receiver ✅

**Endpoint:** `GET /api/transactions/search/receiver/{walletNumber}`

**Test Case:** Search by specific receiver

```bash
curl -X GET "http://localhost:8080/api/transactions/search/receiver/WAL002?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 200 OK
- Only transactions where receiverWalletNumber == "WAL002"

---

### Test 5: Search by Amount Range ✅

**Endpoint:** `GET /api/transactions/search/amount`

**Test Case:** Search by amount range

```bash
curl -X GET "http://localhost:8080/api/transactions/search/amount?min=100&max=1000&sortBy=amount&direction=ASC" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 200 OK
- All transactions with 100 <= amount <= 1000
- Sorted by amount ascending

---

### Test 6: Search by Status ✅

**Endpoint:** `GET /api/transactions/search/status/{status}`

**Test Case:** Search by specific status

```bash
curl -X GET "http://localhost:8080/api/transactions/search/status/COMPLETED?page=0&size=100" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 200 OK
- Only COMPLETED transactions
- Sorted by createdAt DESC (default)

---

### Test 7: Search by Type ✅

**Endpoint:** `GET /api/transactions/search/type/{type}`

**Test Case:** Search by transaction type

```bash
curl -X GET "http://localhost:8080/api/transactions/search/type/TRANSFER?currency=USD&page=0&size=50" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 200 OK
- Only TRANSFER type transactions
- Filtered by USD currency

---

### Test 8: Export CSV ✅

**Endpoint:** `GET /api/transactions/export`

**Test Case:** Export as CSV format

```bash
curl -X GET "http://localhost:8080/api/transactions/export?format=CSV&status=COMPLETED&page=0&size=1000" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o transactions.csv
```

**Expected Response:**
- Status: 200 OK
- Content-Type: text/csv
- Content-Disposition: attachment; filename="transactions_*.csv"
- CSV format with header row
- Data rows with all transaction fields

**CSV Validation:**
```
Header line: ID,Reference,Sender Wallet,...
Data line format: 1,"TXN-001","WAL001","WAL002",100.00,USD,...
Proper escaping of commas in fields
UTF-8 encoding
```

---

### Test 9: Export JSON ✅

**Endpoint:** `GET /api/transactions/export`

**Test Case:** Export as JSON format

```bash
curl -X GET "http://localhost:8080/api/transactions/export?format=JSON&amountMin=100&amountMax=1000&page=0&size=1000" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o transactions.json
```

**Expected Response:**
- Status: 200 OK
- Content-Type: application/json
- Content-Disposition: attachment; filename="transactions_*.json"
- Valid JSON array
- Each object contains all transaction fields

**JSON Validation:**
```javascript
// Response should be:
[
  {
    "id": 1,
    "reference": "TXN-001",
    "amount": 100.00,
    ...
  }
]
// Valid JSON syntax
// All fields properly quoted
// Date fields in ISO format
```

---

### Test 10: Statistics ✅

**Endpoint:** `GET /api/transactions/statistics/status`

**Test Case:** Get transaction count by status

```bash
curl -X GET "http://localhost:8080/api/transactions/statistics/status" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 200 OK
- Content-Type: application/json
- Map of status -> count

**Response Validation:**
```json
{
  "COMPLETED": 1234,
  "PENDING": 45,
  "FAILED": 12,
  ...
}
// All status enum values present (or zero if no transactions)
// Counts are non-negative integers
// Sum of counts >= 0
```

---

## EDGE CASES & ERROR TESTS

### Test 11: Invalid Date Format ❌

**Request:**
```bash
curl -X GET "http://localhost:8080/api/transactions/search?fromDate=2025-12-16" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 400 Bad Request
- Error message mentions date format

---

### Test 12: Invalid Amount (String) ❌

**Request:**
```bash
curl -X GET "http://localhost:8080/api/transactions/search?amountMin=abc" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 400 Bad Request
- Error mentions invalid number

---

### Test 13: Invalid Status Enum ❌

**Request:**
```bash
curl -X GET "http://localhost:8080/api/transactions/search?status=INVALID_STATUS" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 400 Bad Request
- Error mentions invalid enum value

---

### Test 14: Page Size > Max ❌

**Request:**
```bash
curl -X GET "http://localhost:8080/api/transactions/search?size=10000" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 200 OK (capped to 1000)
- OR Status: 400 Bad Request
- Actual page size <= 1000

---

### Test 15: Missing Auth Token ❌

**Request:**
```bash
curl -X GET "http://localhost:8080/api/transactions/search"
```

**Expected Response:**
- Status: 401 Unauthorized
- Error message about missing token

---

## PERFORMANCE TESTS

### Test 16: Large Result Set Performance ✅

**Test:** Export 10K transactions as CSV

```bash
curl -X GET "http://localhost:8080/api/transactions/export?format=CSV&page=0&size=10000" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -w "Time: %{time_total}s" \
  -o large_export.csv
```

**Expected Performance:**
- Response time: < 1 second
- File size: 500KB - 5MB

---

### Test 17: Complex Filter Performance ✅

**Test:** Complex multi-filter search

```bash
curl -X GET "http://localhost:8080/api/transactions/search?senderWallet=WAL001&status=COMPLETED&amountMin=100&amountMax=10000&fromDate=2025-01-01T00:00:00&toDate=2025-12-31T23:59:59&currency=USD&page=0&size=100" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -w "Time: %{time_total}s"
```

**Expected Performance:**
- Response time: < 200ms
- Indexes working correctly

---

### Test 18: Pagination Performance ✅

**Test:** Iterate through all pages

```bash
# Script to paginate through results
for page in {0..100}; do
  curl -X GET "http://localhost:8080/api/transactions/search?page=$page&size=20" \
    -H "Authorization: Bearer YOUR_TOKEN" \
    -w "Page $page - Time: %{time_total}s\n"
done
```

**Expected Performance:**
- Each page: < 100ms
- Consistent response time across pages

---

## REGRESSION TESTS

### Test 19: Existing GET / Endpoint Still Works ✅

**Request:**
```bash
curl -X GET "http://localhost:8080/api/transactions" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 200 OK
- List of all transactions (no pagination - backward compatible)

---

### Test 20: Existing GET /{id} Still Works ✅

**Request:**
```bash
curl -X GET "http://localhost:8080/api/transactions/1" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
- Status: 200 OK
- Single transaction details

---

## DATABASE VERIFICATION

### Verify Indexes Created:

```sql
-- PostgreSQL
SELECT indexname FROM pg_indexes WHERE tablename = 'transactions';

-- Expected output:
idx_transactions_sender_wallet
idx_transactions_receiver_wallet
idx_transactions_reference
idx_transactions_currency
idx_transactions_route
idx_transactions_created_at
idx_transactions_scheduled
idx_transactions_amount_created
idx_transactions_status_created
idx_transactions_type_currency
idx_transactions_sender_status
idx_transactions_receiver_status
idx_transactions_status_date_range
```

---

## POSTMAN COLLECTION

### Create Postman Environment Variables:

```json
{
  "base_url": "http://localhost:8080",
  "api_path": "/api/transactions",
  "token": "YOUR_JWT_TOKEN",
  "page": "0",
  "size": "20"
}
```

### Create Collection with Requests:

```
Transaction Service - Search & Filter
├── GET /search (all parameters)
├── GET /search (no parameters)
├── GET /search/sender/WAL001
├── GET /search/receiver/WAL002
├── GET /search/amount (100-1000)
├── GET /search/status/COMPLETED
├── GET /search/type/TRANSFER
├── GET /export (CSV)
├── GET /export (JSON)
├── GET /statistics/status
├── Error: Invalid Date
├── Error: Missing Auth
└── Performance: Large Export
```

---

## TEST EXECUTION CHECKLIST

- [ ] Unit tests pass (TransactionSearchServiceTest)
- [ ] Integration tests pass (TransactionControllerTest)
- [ ] All 10 positive test cases pass
- [ ] All 5 negative test cases handled properly
- [ ] Performance tests meet targets
- [ ] Regression tests pass
- [ ] Database indexes verified
- [ ] CSV export valid format
- [ ] JSON export valid format
- [ ] Statistics endpoint returns correct counts
- [ ] Authentication required working
- [ ] Pagination working correctly
- [ ] Sorting working correctly
- [ ] No N+1 query problems
- [ ] Memory usage reasonable

---

## SIGN-OFF

**Testing Date:** ___________

**Tester Name:** ___________

**Results:** ☐ PASSED ☐ FAILED

**Issues Found:** 

---

**Next Phase:** Reversal & Refund Services (5-6 days)
