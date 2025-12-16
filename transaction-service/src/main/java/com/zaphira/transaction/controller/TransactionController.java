package com.zaphira.transaction.controller;

import com.zaphira.transaction.dto.AuthorizationInfoResponse;
import com.zaphira.transaction.dto.AuthorizationValidationRequest;
import com.zaphira.transaction.dto.TransactionRequest;
import com.zaphira.transaction.dto.UpdateStatusRequest;
import com.zaphira.transaction.dto.TransactionSearchRequest;
import com.zaphira.transaction.dto.TransactionReversalRequest;
import com.zaphira.transaction.dto.TransactionReversalResponse;
import com.zaphira.transaction.dto.TransactionRefundRequest;
import com.zaphira.transaction.dto.TransactionRefundResponse;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.TransactionStateHistory;
import com.zaphira.transaction.model.TransactionAuditLog;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.service.TransactionService;
import com.zaphira.transaction.service.TransactionSearchService;
import com.zaphira.transaction.service.TransactionReversalService;
import com.zaphira.transaction.service.TransactionRefundService;
import com.zaphira.transaction.repository.TransactionAuditLogRepository;
import com.zaphira.transaction.security.AuthenticatedUser;

import com.zaphira.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionSearchService transactionSearchService;
    private final TransactionReversalService transactionReversalService;
    private final TransactionRefundService transactionRefundService;
    private final TransactionAuditLogRepository transactionAuditLogRepository;

    public TransactionController(
        TransactionService transactionService,
        TransactionSearchService transactionSearchService,
        TransactionReversalService transactionReversalService,
        TransactionRefundService transactionRefundService,
        TransactionAuditLogRepository transactionAuditLogRepository) {
        this.transactionService = transactionService;
        this.transactionSearchService = transactionSearchService;
        this.transactionReversalService = transactionReversalService;
        this.transactionRefundService = transactionRefundService;
        this.transactionAuditLogRepository = transactionAuditLogRepository;
    }

    // ============================================================
    // ========== HELPER METHODS FOR JWT EXTRACTION ==============
    // ============================================================

    /**
     * Extract authenticated user from JWT/SecurityContext.
     * The JwtAuthenticationFilter sets AuthenticatedUser as principal.
     * 
     * @return AuthenticatedUser extracted from current security context
     * @throws IllegalStateException if no authenticated user found
     */
    private AuthenticatedUser getAuthenticatedUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found in security context");
        }

        var principal = auth.getPrincipal();
        if (principal instanceof AuthenticatedUser) {
            return (AuthenticatedUser) principal;
        }

        throw new IllegalStateException("Principal is not of type AuthenticatedUser. Type: " + principal.getClass().getSimpleName());
    }

    /**
     * Extract user roles from authentication authorities.
     * Only returns authorities with 'ROLE_' prefix.
     * 
     * @return List of role names (with ROLE_ prefix)
     */
    private java.util.List<String> getUserRoles() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication().getAuthorities().stream()
            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
            .filter(auth -> auth.startsWith("ROLE_"))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Extract user permissions from authentication authorities.
     * Only returns authorities that are NOT role-based.
     * 
     * @return List of permission names
     */
    private java.util.List<String> getUserPermissions() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication().getAuthorities().stream()
            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
            .filter(auth -> !auth.startsWith("ROLE_"))
            .collect(java.util.stream.Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody TransactionRequest request) {
        Transaction saved = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id) {
        Transaction tx = transactionService.getTransaction(id);
        return ResponseEntity.ok(tx);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TransactionStateHistory>> getTransactionHistory(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(id));
    }

    @GetMapping("/{id}/authorization")
    public ResponseEntity<AuthorizationInfoResponse> getAuthorizationInfo(@PathVariable Long id) {
        AuthorizationInfoResponse response = transactionService.getAuthorizationInfo(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/authorize")
    public ResponseEntity<Transaction> authorizeTransaction(@PathVariable Long id,
                                                            @Valid @RequestBody AuthorizationValidationRequest request) {
        Transaction tx = transactionService.authorizeTransaction(id, request);
        return ResponseEntity.ok(tx);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Transaction> updateStatus(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateStatusRequest request) {
        Transaction updated = transactionService.updateTransactionStatus(id, request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Transaction> cancelTransaction(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateStatusRequest request) {
        Transaction cancelled = transactionService.cancelTransaction(id, request);
        return ResponseEntity.ok(cancelled);
    }

    // ============================================================
    // ADVANCED SEARCH & FILTERING ENDPOINTS
    // ============================================================

    /**
     * Advanced search with multiple filter criteria.
     * Supports filtering by: wallet, status, type, amount range, date range, currency, reference, route
     *
     * Example: GET /api/transactions/search?senderWallet=WAL001&status=COMPLETED&amountMin=100&page=0&size=20&sortBy=createdAt&direction=DESC
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Transaction>> searchTransactions(
            @RequestParam(required = false) String senderWallet,
            @RequestParam(required = false) String receiverWallet,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String route,
            @RequestParam(required = false) Boolean scheduled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("Search request - senderWallet: {}, status: {}, amountMin: {}, amountMax: {}, page: {}, size: {}",
                senderWallet, status, amountMin, amountMax, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        TransactionSearchRequest searchRequest = TransactionSearchRequest.builder()
                .senderWalletNumber(senderWallet)
                .receiverWalletNumber(receiverWallet)
                .status(status)
                .type(type)
                .amountMin(amountMin)
                .amountMax(amountMax)
                .createdFrom(fromDate)
                .createdTo(toDate)
                .reference(reference)
                .currency(currency)
                .route(route)
                .scheduled(scheduled)
                .build();

        Page<Transaction> results = transactionSearchService.search(searchRequest, pageable);
        log.info("Search returned {} results", results.getTotalElements());
        return ResponseEntity.ok(results);
    }

    /**
     * Search transactions by sender wallet.
     * Example: GET /api/transactions/search/sender/WAL001?page=0&size=20
     */
    @GetMapping("/search/sender/{walletNumber}")
    public ResponseEntity<Page<Transaction>> searchBySender(
            @PathVariable String walletNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("Search by sender wallet: {}", walletNumber);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Transaction> results = transactionSearchService.searchBySender(walletNumber, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Search transactions by receiver wallet.
     * Example: GET /api/transactions/search/receiver/WAL002?page=0&size=20
     */
    @GetMapping("/search/receiver/{walletNumber}")
    public ResponseEntity<Page<Transaction>> searchByReceiver(
            @PathVariable String walletNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("Search by receiver wallet: {}", walletNumber);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Transaction> results = transactionSearchService.searchByReceiver(walletNumber, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Search transactions by amount range.
     * Example: GET /api/transactions/search/amount?min=100&max=1000&page=0&size=20
     */
    @GetMapping("/search/amount")
    public ResponseEntity<Page<Transaction>> searchByAmount(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("Search by amount range: {} to {}", min, max);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Transaction> results = transactionSearchService.searchByAmountRange(min, max, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Search transactions by status and date range.
     * Example: GET /api/transactions/search/status/COMPLETED?from=2025-01-01T00:00:00&to=2025-12-31T23:59:59&page=0&size=20
     */
    @GetMapping("/search/status/{status}")
    public ResponseEntity<Page<Transaction>> searchByStatus(
            @PathVariable TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("Search by status: {} from {} to {}", status, from, to);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Transaction> results = transactionSearchService.searchByStatusAndDate(status, from, to, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Search transactions by type and currency.
     * Example: GET /api/transactions/search/type/TRANSFER?currency=USD&page=0&size=20
     */
    @GetMapping("/search/type/{type}")
    public ResponseEntity<Page<Transaction>> searchByType(
            @PathVariable TransactionType type,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("Search by type: {} currency: {}", type, currency);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        if (currency != null) {
            Page<Transaction> results = transactionSearchService.searchByTypeAndCurrency(type, currency, pageable);
            return ResponseEntity.ok(results);
        }

        return ResponseEntity.ok(Page.empty(pageable));
    }

    /**
     * Export transactions to CSV or JSON format.
     * Supports filtering with the same criteria as /search endpoint
     *
     * Example: GET /api/transactions/export?format=CSV&status=COMPLETED
     * Example: GET /api/transactions/export?format=JSON&amountMin=100&amountMax=1000
     */
    @GetMapping("/export")
    public ResponseEntity<Resource> exportTransactions(
            @RequestParam(defaultValue = "CSV") String format,
            @RequestParam(required = false) String senderWallet,
            @RequestParam(required = false) String receiverWallet,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) throws IOException {

        log.info("Export request - format: {}, status: {}, page: {}, size: {}", format, status, page, size);

        TransactionSearchRequest searchRequest = TransactionSearchRequest.builder()
                .senderWalletNumber(senderWallet)
                .receiverWalletNumber(receiverWallet)
                .status(status)
                .type(type)
                .amountMin(amountMin)
                .amountMax(amountMax)
                .createdFrom(fromDate)
                .createdTo(toDate)
                .currency(currency)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        Resource resource = transactionSearchService.exportTransactions(searchRequest, format, pageable);

        String contentType = "JSON".equalsIgnoreCase(format)
                ? MediaType.APPLICATION_JSON_VALUE
                : "text/csv";

        String filename = "transactions_" + System.currentTimeMillis() +
                ("JSON".equalsIgnoreCase(format) ? ".json" : ".csv");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Get transaction statistics by status.
     * Returns count of transactions in each status.
     *
     * Example: GET /api/transactions/statistics/status
     */
    @GetMapping("/statistics/status")
    public ResponseEntity<Map<String, Long>> getStatusStatistics() {
        log.info("Getting transaction status statistics");
        Map<String, Long> statistics = transactionSearchService.getStatusStatistics();
        return ResponseEntity.ok(statistics);
    }

    // ============================================================
    // ======= PHASE 2: REVERSAL & REFUND ENDPOINTS (SECURE) ======
    // ============================================================

    /**
     * Reverse a transaction completely.
     * 
     * SECURITY: Only ADMIN and SUPPORT roles with TRANSACTION_REVERSE permission
     * 
     * @PreAuthorize checks:
     * 1. JWT token must be present and valid
     * 2. User must have TRANSACTION_REVERSE authority
     * 3. AuthenticatedUser must be extractable from SecurityContext
     * 
     * Additional checks in service:
     * 1. Transaction status must be COMPLETED or PROCESSING
     * 2. Transaction must be within 30 days of creation
     * 3. User role must be ADMIN or SUPPORT
     * 4. Reversal amount must not exceed original amount
     * 5. Transaction must not already be REVERSED or REFUNDED
     * 6. Complete audit logging with actor details
     * 7. Atomic wallet balance updates
     * 
     * Example cURL:
     * curl -X POST http://localhost:8080/api/transactions/1/reverse \
     *   -H "Authorization: Bearer JWT_TOKEN" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "transactionId": 1,
     *     "reason": "Duplicate transaction",
     *     "amount": 100.00,
     *     "includesFees": false
     *   }'
     */
    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAuthority('TRANSACTION_REVERSE')")
    public ResponseEntity<?> reverseTransaction(
        @PathVariable Long id,
        @Valid @RequestBody TransactionReversalRequest request,
        jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        try {
            log.info("Processing reversal request for transaction {} by user", id);
            
            // Extract authenticated user from SecurityContext (set by JwtAuthenticationFilter)
            AuthenticatedUser user = getAuthenticatedUser();
            log.debug("Reversal initiated by user: {} ({})", user.getId(), user.getEmail());
            
            // Extract roles and permissions from JWT authorities
            java.util.List<String> userRoles = getUserRoles();
            java.util.List<String> userPermissions = getUserPermissions();
            log.debug("User roles: {}, permissions: {}", userRoles, userPermissions);
            
            // Extract request metadata for audit trail
            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            String requestId = org.springframework.web.context.request.RequestContextHolder
                .currentRequestAttributes().getSessionId();
            
            log.debug("Request metadata - IP: {}, UserAgent: {}, RequestId: {}", ipAddress, userAgent, requestId);
            
            // Call the reversal service with complete user context
            TransactionReversalResponse response = transactionReversalService.reverse(
                user, userRoles, userPermissions, request, ipAddress, userAgent, requestId
            );
            
            log.info("Reversal successful for transaction {} -> reversal transaction {}", id, response.getReversalTransactionId());
            return ResponseEntity.ok(response);
            
        } catch (com.zaphira.transaction.exception.AccessDeniedException e) {
            log.warn("Access denied for reversal of transaction {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "error", "Access Denied",
                    "message", e.getMessage(),
                    "transactionId", id
                ));
        } catch (ResourceNotFoundException e) {
            log.error("Transaction not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "error", "Not Found",
                    "message", e.getMessage(),
                    "transactionId", id
                ));
        } catch (IllegalStateException e) {
            log.error("Security context error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid or missing authentication"
                ));
        } catch (Exception e) {
            log.error("Error processing reversal for transaction {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage(),
                    "transactionId", id
                ));
        }
    }

    /**
     * Refund a transaction (full or partial).
     * 
     * SECURITY: Only authorized users with TRANSACTION_REFUND permission
     * 
     * @PreAuthorize checks:
     * 1. JWT token must be present and valid
     * 2. User must have TRANSACTION_REFUND authority
     * 3. AuthenticatedUser must be extractable from SecurityContext
     * 
     * Additional checks in service:
     * 1. If partial refund: requires TRANSACTION_REFUND_PARTIAL permission
     * 2. Transaction status must be COMPLETED
     * 3. Transaction must be within 90 days of creation
     * 4. For MERCHANT role: can only refund own transactions
     * 5. For SUPPORT role: limited to max amount (e.g., 5000.00)
     * 6. Refund amount must not exceed original amount
     * 7. Transaction must not already be REVERSED or REFUNDED
     * 8. Complete audit logging with actor details
     * 9. Atomic wallet balance updates
     * 
     * Supports:
     * - Full refund (100% of amount)
     * - Partial refund (less than 100%)
     * - Multiple partial refunds in sequence
     * - Refunding fees separately
     * 
     * Example cURL (Full Refund):
     * curl -X POST http://localhost:8080/api/transactions/1/refund \
     *   -H "Authorization: Bearer JWT_TOKEN" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "transactionId": 1,
     *     "refundAmount": 100.00,
     *     "reason": "Customer request",
     *     "includeFees": true,
     *     "refundType": "FULL"
     *   }'
     * 
     * Example cURL (Partial Refund):
     * curl -X POST http://localhost:8080/api/transactions/1/refund \
     *   -H "Authorization: Bearer JWT_TOKEN" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "transactionId": 1,
     *     "refundAmount": 50.00,
     *     "reason": "Partial return",
     *     "includeFees": false,
     *     "refundType": "PARTIAL"
     *   }'
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('TRANSACTION_REFUND')")
    public ResponseEntity<?> refundTransaction(
        @PathVariable Long id,
        @Valid @RequestBody TransactionRefundRequest request,
        jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        try {
            log.info("Processing refund request for transaction {} - amount: {}, type: {}", 
                id, request.getRefundAmount(), request.getRefundType());
            
            // Extract authenticated user from SecurityContext (set by JwtAuthenticationFilter)
            AuthenticatedUser user = getAuthenticatedUser();
            log.debug("Refund initiated by user: {} ({})", user.getId(), user.getEmail());
            
            // Extract roles and permissions from JWT authorities
            java.util.List<String> userRoles = getUserRoles();
            java.util.List<String> userPermissions = getUserPermissions();
            log.debug("User roles: {}, permissions: {}", userRoles, userPermissions);
            
            // Extract request metadata for audit trail
            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            String requestId = org.springframework.web.context.request.RequestContextHolder
                .currentRequestAttributes().getSessionId();
            
            log.debug("Request metadata - IP: {}, UserAgent: {}, RequestId: {}", ipAddress, userAgent, requestId);
            
            // Call the refund service with complete user context
            TransactionRefundResponse response = transactionRefundService.refund(
                user, userRoles, userPermissions, request, ipAddress, userAgent, requestId
            );
            
            log.info("Refund successful for transaction {} - refund transaction: {}, amount: {}", 
                id, response.getRefundTransactionId(), response.getTotalRefundAmount());
            return ResponseEntity.ok(response);
            
        } catch (com.zaphira.transaction.exception.AccessDeniedException e) {
            log.warn("Access denied for refund of transaction {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "error", "Access Denied",
                    "message", e.getMessage(),
                    "transactionId", id
                ));
        } catch (ResourceNotFoundException e) {
            log.error("Transaction not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "error", "Not Found",
                    "message", e.getMessage(),
                    "transactionId", id
                ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid refund request for transaction {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "error", "Bad Request",
                    "message", e.getMessage(),
                    "transactionId", id
                ));
        } catch (IllegalStateException e) {
            log.error("Security context error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid or missing authentication"
                ));
        } catch (Exception e) {
            log.error("Error processing refund for transaction {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage(),
                    "transactionId", id
                ));
        }
    }

    /**
     * Get audit logs for a transaction.
     * 
     * SECURITY: Only ADMIN and SUPPORT can access audit logs
     * 
     * Example cURL:
     * curl -X GET "http://localhost:8080/api/transactions/1/audit-logs?page=0&size=20" \
     *   -H "Authorization: Bearer JWT_TOKEN"
     */
    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<Page<TransactionAuditLog>> getAuditLogs(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        
        log.info("Fetching audit logs for transaction {}", id);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<TransactionAuditLog> auditLogs = transactionAuditLogRepository.findByTransactionId(id, pageable);
        
        return ResponseEntity.ok(auditLogs);
    }
}


