# PHASE 3 - TEST CASES DOCUMENTATION

## Test Suites Planned

### 1. DisputeService Tests (12 test cases)

```java
@SpringBootTest
class DisputeServiceTests {
    
    // CREATE DISPUTE TESTS
    
    @Test
    void testCreateDispute_Success() {
        // Customer creates dispute for valid transaction
        // Expected: Dispute created with INITIATED status
        // Verify: Timeline event created, Kafka event published
    }
    
    @Test
    void testCreateDispute_TransactionNotFound() {
        // Customer tries to dispute non-existent transaction
        // Expected: ResourceNotFoundException thrown
        // HTTP: 404 Not Found
    }
    
    @Test
    void testCreateDispute_CustomerNotOwner() {
        // Customer A tries to dispute Customer B's transaction
        // Expected: AccessDeniedException thrown
        // HTTP: 403 Forbidden
    }
    
    @Test
    void testCreateDispute_TransactionTooOld() {
        // Customer tries to dispute transaction > 180 days old
        // Expected: AccessDeniedException thrown
        // HTTP: 403 Forbidden
    }
    
    @Test
    void testCreateDispute_DuplicateDispute() {
        // Customer tries to create second dispute for same transaction
        // Expected: ValidationException thrown
        // HTTP: 400 Bad Request
    }
    
    @Test
    void testCreateDispute_InvalidAmount() {
        // Customer creates dispute with amount > transaction amount
        // Expected: ValidationException thrown
        // HTTP: 400 Bad Request
    }
    
    // SUBMIT EVIDENCE TESTS
    
    @Test
    void testSubmitEvidence_Success() {
        // Customer submits receipt as evidence
        // Expected: DisputeEvidence created, Timeline event added
        // File uploaded and stored (mocked)
    }
    
    @Test
    void testSubmitEvidence_DeadlinePassed() {
        // Evidence submission after 14-day deadline
        // Expected: ValidationException thrown
        // HTTP: 400 Bad Request
    }
    
    @Test
    void testSubmitEvidence_FileTooLarge() {
        // Customer tries to upload file > 50MB
        // Expected: ValidationException thrown
        // HTTP: 400 Bad Request
    }
    
    @Test
    void testSubmitEvidence_InvalidFileType() {
        // Customer tries to upload .exe file
        // Expected: ValidationException thrown
        // HTTP: 400 Bad Request
    }
    
    // RESPOND TO DISPUTE TESTS
    
    @Test
    void testRespondToDispute_Success() {
        // Merchant responds with delivery proof
        // Expected: Evidence added, Status → AWAITING_RESPONSE
        // Timeline event created
    }
    
    @Test
    void testRespondToDispute_NotMerchant() {
        // Non-merchant tries to respond
        // Expected: AccessDeniedException thrown
        // HTTP: 403 Forbidden
    }
}
```

### 2. DisputeAuthorizationService Tests (15 test cases)

```java
@SpringBootTest
class DisputeAuthorizationServiceTests {
    
    // AUTHORIZE CREATION TESTS
    
    @Test
    void testAuthorizeCreation_CustomerOwnsTransaction() {
        // Customer creates dispute for their own transaction
        // Expected: Authorization passes
    }
    
    @Test
    void testAuthorizeCreation_AdminCanCreateForOthers() {
        // Admin creates dispute for any customer
        // Expected: Authorization passes
    }
    
    @Test
    void testAuthorizeCreation_MerchantCannotCreate() {
        // Merchant tries to create dispute
        // Expected: AccessDeniedException thrown
    }
    
    @Test
    void testAuthorizeCreation_TransactionNotCompleted() {
        // Dispute for transaction in PENDING status
        // Expected: AccessDeniedException thrown
    }
    
    @Test
    void testAuthorizeCreation_MinimumAmountCheck() {
        // Dispute for amount < $1.00
        // Expected: AccessDeniedException thrown
    }
    
    // AUTHORIZE EVIDENCE SUBMISSION TESTS
    
    @Test
    void testAuthorizeEvidence_DisputeInitiatorCanSubmit() {
        // Dispute creator submits evidence
        // Expected: Authorization passes
    }
    
    @Test
    void testAuthorizeEvidence_AdminCanAlwaysSubmit() {
        // Admin submits evidence to any dispute
        // Expected: Authorization passes
    }
    
    @Test
    void testAuthorizeEvidence_NonInitiatorCannotSubmit() {
        // Third party tries to submit evidence
        // Expected: AccessDeniedException thrown
    }
    
    @Test
    void testAuthorizeEvidence_DeadlineExpired() {
        // Submission after deadline_at
        // Expected: AccessDeniedException thrown
    }
    
    @Test
    void testAuthorizeEvidence_DisputeResolved() {
        // Evidence submission for already RESOLVED dispute
        // Expected: AccessDeniedException thrown
    }
    
    // AUTHORIZE DISPUTE RESPONSE TESTS
    
    @Test
    void testAuthorizeResponse_MerchantCanRespond() {
        // Merchant responds to dispute
        // Expected: Authorization passes
    }
    
    @Test
    void testAuthorizeResponse_AdminCanRespond() {
        // Admin responds to dispute
        // Expected: Authorization passes
    }
    
    @Test
    void testAuthorizeResponse_CustomerCannotRespond() {
        // Customer tries to respond (merchant-only)
        // Expected: AccessDeniedException thrown
    }
    
    @Test
    void testAuthorizeResponse_InvalidStatus() {
        // Response to dispute in UNDER_INVESTIGATION status
        // Expected: AccessDeniedException thrown
    }
    
    // AUTHORIZE RESOLUTION TESTS
    
    @Test
    void testAuthorizeResolution_AdminOnly() {
        // Non-admin tries to resolve dispute
        // Expected: AccessDeniedException thrown
    }
}
```

### 3. DisputeResolutionService Tests (10 test cases)

```java
@SpringBootTest
class DisputeResolutionServiceTests {
    
    // HOLD AMOUNT TESTS
    
    @Test
    void testHoldAmount_Success() {
        // Funds placed in escrow
        // Expected: Wallet updated, amount held
    }
    
    @Test
    void testHoldAmount_InsufficientBalance() {
        // Try to hold amount exceeding balance
        // Expected: BusinessException thrown
        // HTTP: 409 Conflict
    }
    
    // RESOLVE DISPUTE TESTS
    
    @Test
    void testResolveDispute_ApprovedRefund() {
        // Admin approves full refund
        // Expected: Full amount released to customer
        // Status → RESOLVED
        // Kafka event published
    }
    
    @Test
    void testResolveDispute_PartialApproval() {
        // Admin approves partial refund
        // Expected: Partial to customer, remainder to merchant
    }
    
    @Test
    void testResolveDispute_Denied() {
        // Admin denies dispute
        // Expected: Full amount released to merchant
        // Resolution amount = 0
    }
    
    @Test
    void testResolveDispute_Settlement() {
        // Admin approves settlement amount
        // Expected: Settlement amount to customer
    }
    
    @Test
    void testResolveDispute_Withdrawn() {
        // Customer withdraws dispute
        // Expected: Amount released to merchant
    }
    
    @Test
    void testResolveDispute_Expired() {
        // Dispute expired after deadline
        // Expected: Amount released to merchant
    }
    
    @Test
    void testResolveDispute_InvalidAmount() {
        // Resolution amount > claimed amount
        // Expected: BusinessException thrown
    }
    
    @Test
    void testResolveDispute_AlreadyResolved() {
        // Admin tries to resolve already RESOLVED dispute
        // Expected: AccessDeniedException thrown
    }
}
```

### 4. DisputeController Integration Tests (18 test cases)

```java
@SpringBootTest
@AutoConfigureMockMvc
class DisputeControllerIntegrationTests {
    
    // CREATE DISPUTE ENDPOINT TESTS
    
    @Test
    void testCreateDispute_201Created() {
        // POST /api/disputes
        // Expected: 201 Created + DisputeResponse
    }
    
    @Test
    void testCreateDispute_400BadRequest() {
        // Invalid request body (missing required fields)
        // Expected: 400 Bad Request
    }
    
    @Test
    void testCreateDispute_403Forbidden() {
        // Not authorized
        // Expected: 403 Forbidden
    }
    
    @Test
    void testCreateDispute_404NotFound() {
        // Transaction doesn't exist
        // Expected: 404 Not Found
    }
    
    @Test
    void testCreateDispute_409Conflict() {
        // Dispute already exists for transaction
        // Expected: 409 Conflict
    }
    
    // SUBMIT EVIDENCE ENDPOINT TESTS
    
    @Test
    void testSubmitEvidence_200OK() {
        // POST /api/disputes/{id}/evidence
        // Multipart form-data with file
        // Expected: 200 OK + evidence details
    }
    
    @Test
    void testSubmitEvidence_400BadRequest() {
        // Missing file or invalid file type
        // Expected: 400 Bad Request
    }
    
    @Test
    void testSubmitEvidence_403Forbidden() {
        // User not authorized for this dispute
        // Expected: 403 Forbidden
    }
    
    @Test
    void testSubmitEvidence_404NotFound() {
        // Dispute doesn't exist
        // Expected: 404 Not Found
    }
    
    // RESPOND TO DISPUTE ENDPOINT TESTS
    
    @Test
    void testRespondToDispute_200OK() {
        // POST /api/disputes/{id}/respond
        // Merchant response with counter-evidence
        // Expected: 200 OK + updated dispute
    }
    
    @Test
    void testRespondToDispute_403Forbidden() {
        // Non-merchant tries to respond
        // Expected: 403 Forbidden
    }
    
    // GET DISPUTE DETAILS ENDPOINT TESTS
    
    @Test
    void testGetDisputeDetails_200OK() {
        // GET /api/disputes/{id}
        // Expected: 200 OK + DisputeResponse with full details
    }
    
    @Test
    void testGetDisputeDetails_403Forbidden() {
        // User not authorized to view
        // Expected: 403 Forbidden
    }
    
    @Test
    void testGetDisputeDetails_404NotFound() {
        // Dispute doesn't exist
        // Expected: 404 Not Found
    }
    
    // RESOLVE DISPUTE ENDPOINT TESTS
    
    @Test
    void testResolveDispute_200OK() {
        // PUT /api/disputes/{id}/resolve
        // Admin resolution decision
        // Expected: 200 OK + resolved dispute
    }
    
    @Test
    void testResolveDispute_403Forbidden() {
        // Non-admin tries to resolve
        // Expected: 403 Forbidden
    }
    
    @Test
    void testResolveDispute_409Conflict() {
        // Invalid resolution amount
        // Expected: 409 Conflict
    }
}
```

### 5. Database Tests (8 test cases)

```java
@DataJpaTest
class DisputeRepositoryTests {
    
    @Test
    void testFindByTransactionId_Success() {
        // Retrieve dispute by transaction ID
        // Expected: Dispute found
    }
    
    @Test
    void testFindByTransactionId_NotFound() {
        // Transaction with no dispute
        // Expected: Optional.empty()
    }
    
    @Test
    void testFindByStatus_MultipleResults() {
        // Find all disputes with status INITIATED
        // Expected: List of matching disputes
    }
    
    @Test
    void testFindByInitiatedBy_CustomerDisputes() {
        // Get disputes created by customer
        // Expected: List of customer's disputes
    }
    
    @Test
    void testFindWithApproachingDeadline() {
        // Get disputes with deadline < NOW + 3 days
        // Expected: List of disputes approaching deadline
    }
    
    @Test
    void testFindUnresolvedDisputes() {
        // Get all open disputes
        // Expected: List excluding RESOLVED, CLOSED, EXPIRED
    }
    
    @Test
    void testCascadeDeleteEvidence() {
        // Delete dispute should cascade to evidence
        // Expected: All evidence deleted with dispute
    }
    
    @Test
    void testCascadeDeleteTimeline() {
        // Delete dispute should cascade to timeline
        // Expected: All timeline events deleted with dispute
    }
}
```

### 6. Kafka Event Tests (6 test cases)

```java
@SpringBootTest
class KafkaEventTests {
    
    @Test
    void testDisputeCreatedEvent_Published() {
        // Dispute creation publishes event to Kafka
        // Expected: Event in "dispute-created" topic
        // Verify all fields populated
    }
    
    @Test
    void testDisputeCreatedEvent_Structure() {
        // Event has correct structure
        // Expected: All required fields (disputeId, transactionId, claimedAmount, etc)
    }
    
    @Test
    void testDisputeResolvedEvent_Published() {
        // Dispute resolution publishes event to Kafka
        // Expected: Event in "dispute-resolved" topic
    }
    
    @Test
    void testDisputeResolvedEvent_Structure() {
        // Event has correct structure
        // Expected: All required fields (resolutionType, amount, etc)
    }
    
    @Test
    void testKafkaMessageKey() {
        // Message key is dispute reference
        // Expected: Key = dispute.getReference() (DSP-ABC123)
    }
    
    @Test
    void testEventConsumption() {
        // External service receives and processes event
        // Expected: Event consumed and processed correctly
    }
}
```

### 7. Security & Authorization Tests (10 test cases)

```java
@SpringBootTest
class SecurityTests {
    
    @Test
    void testJWTExtraction_ValidToken() {
        // Valid JWT token extracted
        // Expected: AuthenticatedUser populated from token
    }
    
    @Test
    void testJWTExtraction_InvalidToken() {
        // Invalid JWT token
        // Expected: Authentication fails
    }
    
    @Test
    void testJWTExtraction_ExpiredToken() {
        // Expired JWT token
        // Expected: Authentication fails
    }
    
    @Test
    void testRoleBasedAccess_Customer() {
        // Customer accessing customer endpoints
        // Expected: Access granted
    }
    
    @Test
    void testRoleBasedAccess_Merchant() {
        // Merchant accessing merchant endpoints
        // Expected: Access granted
    }
    
    @Test
    void testRoleBasedAccess_Admin() {
        // Admin accessing all endpoints
        // Expected: Access granted to all
    }
    
    @Test
    void testPermissionCheck_Granted() {
        // User with DISPUTE_RESOLVE permission
        // Expected: Can resolve disputes
    }
    
    @Test
    void testPermissionCheck_Denied() {
        // User without required permission
        // Expected: Access denied
    }
    
    @Test
    void testMultiRoleAccess() {
        // User with multiple roles
        // Expected: Access based on any matching role
    }
    
    @Test
    void testAuditLogging_RequestTracking() {
        // Request ID tracked through system
        // Expected: Found in logs and database records
    }
}
```

### 8. Performance & Load Tests (6 test cases)

```java
@SpringBootTest
class PerformanceTests {
    
    @Test
    void testCreateDispute_ResponseTime() {
        // Dispute creation performance
        // Expected: < 500ms for typical operation
    }
    
    @Test
    void testLargeDatasetQuery() {
        // Query with 100K+ disputes
        // Expected: Uses indexes, completes < 1s
    }
    
    @Test
    void testConcurrentDisputeCreation() {
        // 100 concurrent dispute creations
        // Expected: All succeed, no race conditions
    }
    
    @Test
    void testEvidenceUploadPerformance() {
        // Upload 50MB file
        // Expected: Completed without errors, < 5s
    }
    
    @Test
    void testKafkaEventThroughput() {
        // Publish 1000 events per second
        // Expected: All events delivered, no loss
    }
    
    @Test
    void testDatabaseIndexPerformance() {
        // Verify all indexes used correctly
        // Expected: Query plans show index usage
    }
}
```

---

## Test Execution Strategy

### Phase 1: Unit Tests (IMMEDIATE)
```
DisputeService:                 12 tests
DisputeAuthorizationService:    15 tests
DisputeResolutionService:       10 tests
────────────────────────────────────────
Subtotal:                       37 unit tests
Expected Coverage:              >85%
Expected Time:                  2-3 hours
```

### Phase 2: Integration Tests (NEXT)
```
DisputeController:              18 tests
DisputeRepository:              8 tests
────────────────────────────────────────
Subtotal:                       26 integration tests
Expected Coverage:              >90%
Expected Time:                  3-4 hours
```

### Phase 3: Functional Tests (NEXT)
```
Kafka Events:                   6 tests
Security & Auth:                10 tests
Performance & Load:             6 tests
────────────────────────────────────────
Subtotal:                       22 functional tests
Expected Coverage:              >95%
Expected Time:                  4-5 hours
```

### Total Test Suite
```
Unit Tests:                     37
Integration Tests:              26
Functional Tests:               22
────────────────────────────────────────
TOTAL:                          85+ tests
Expected Coverage:              >90%
Expected Execution Time:        9-12 hours
Continuous Integration:         Jenkins/GitHub Actions
```

---

## Test Data Setup

### Sample Disputes
```java
// Valid dispute
{
    "transactionId": "TXN-TEST-001",
    "category": "FRAUDULENT_TRANSACTION",
    "reason": "I did not authorize this transaction",
    "claimed_amount": 250.00,
    "currency": "USD"
}

// Admin dispute creation on behalf
{
    "transactionId": "TXN-TEST-002",
    "category": "SERVICE_NOT_PROVIDED",
    "reason": "Service not delivered",
    "claimed_amount": 100.00,
    "currency": "USD"
}
```

### Sample Evidence
```
- receipt.pdf (15KB)
- delivery_proof.jpg (2.5MB)
- communication.zip (500KB)
- invoice.docx (250KB)
```

---

## Success Criteria

```
✅ All 85+ tests pass
✅ Code coverage > 90%
✅ No compilation errors
✅ No performance degradation
✅ All security tests pass
✅ Kafka events delivered successfully
✅ Database transactions atomic
✅ Load test completes successfully
```

---

**Test plan ready for implementation** ✅
