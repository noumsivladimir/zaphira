package com.zaphira.transaction.controller;

import com.zaphira.transaction.dto.core.CreateTransactionCoreRequest;
import com.zaphira.transaction.dto.core.TransactionCoreDTO;
import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.requests.BulkTransferRequest;
import com.zaphira.transaction.dto.requests.SplitPaymentRequest;
import com.zaphira.transaction.dto.response.SplitPaymentResponse;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.service.TransactionCoreService;
import com.zaphira.transaction.service.TransactionServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TransactionCoreController - LOT 1 + LOT 2 API Controller
 * 
 * LOT 1 Endpoints:
 * - POST /transfer: P2P transfer
 * - POST /deposit: Deposit to wallet
 * - POST /withdrawal: Withdraw from wallet
 * - GET /{ref}: Get transaction by reference
 * - GET /wallet/{id}: Get wallet transaction history
 * 
 * LOT 2 Endpoints (NEW):
 * - POST /merchant-payment: Merchant payment with fees
 * - POST /{ref}/cancel: Cancel pending transaction
 * - POST /{ref}/retry: Retry failed transaction
 * 
 * Security:
 * - All endpoints require authentication
 * - REGULAR and MERCHANT users can create transactions
 * - ADMIN can view all transactions
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionCoreController {

    private final TransactionCoreService transactionCoreService;
    private final TransactionServiceImpl transactionService;

    /* =========================
       TRANSACTION CREATION
       ========================= */

    /**
     * Create P2P Transfer
     * 
     * POST /api/v1/transactions/transfer
     * 
     * Request:
     * {
     *   "senderWalletId": 1,
     *   "receiverWalletId": 2,
     *   "amount": 100.00,
     *   "currency": "EUR",
     *   "description": "Payment for services"
     * }
     * 
     * Response: 201 Created with TransactionCoreDTO
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<TransactionCoreDTO> transfer(@Valid @RequestBody CreateTransactionCoreRequest request) {
        log.info("POST /api/v1/transactions/transfer - Creating transfer from wallet {} to wallet {}", 
                request.getSenderWalletId(), request.getReceiverWalletId());
        
        TransactionCoreDTO transaction = transactionCoreService.createTransfer(request);
        
        log.info("Transfer created successfully: {}", transaction.getReference());
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    /**
     * Create Deposit
     * 
     * POST /api/v1/transactions/deposit
     * 
     * Request:
     * {
     *   "receiverWalletId": 1,
     *   "amount": 500.00,
     *   "currency": "EUR",
     *   "description": "Initial deposit"
     * }
     * 
     * Response: 201 Created with TransactionCoreDTO
     */
    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<TransactionCoreDTO> deposit(
            @RequestParam @NotNull Long receiverWalletId,
            @RequestParam @NotNull @Positive BigDecimal amount,
            @RequestParam @NotNull String currency,
            @RequestParam(required = false) String description) {
        
        log.info("POST /api/v1/transactions/deposit - Creating deposit for wallet {} amount {}", 
                receiverWalletId, amount);
        
        TransactionCoreDTO transaction = transactionCoreService.createDeposit(
                receiverWalletId, amount, currency, description);
        
        log.info("Deposit created successfully: {}", transaction.getReference());
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    /**
     * Create Withdrawal
     * 
     * POST /api/v1/transactions/withdrawal
     * 
     * Request:
     * {
     *   "senderWalletId": 1,
     *   "amount": 200.00,
     *   "currency": "EUR",
     *   "description": "Cash withdrawal"
     * }
     * 
     * Response: 201 Created with TransactionCoreDTO
     */
    @PostMapping("/withdrawal")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<TransactionCoreDTO> withdrawal(
            @RequestParam @NotNull Long senderWalletId,
            @RequestParam @NotNull @Positive BigDecimal amount,
            @RequestParam @NotNull String currency,
            @RequestParam(required = false) String description) {
        
        log.info("POST /api/v1/transactions/withdrawal - Creating withdrawal from wallet {} amount {}", 
                senderWalletId, amount);
        
        TransactionCoreDTO transaction = transactionCoreService.createWithdrawal(
                senderWalletId, amount, currency, description);
        
        log.info("Withdrawal created successfully: {}", transaction.getReference());
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    /* =========================
       QUERY OPERATIONS
       ========================= */

    /**
     * Get transaction by reference
     * 
     * GET /api/v1/transactions/{reference}
     * 
     * Example: GET /api/v1/transactions/TXN-20260203125500-123456
     * 
     * Response: 200 OK with TransactionCoreDTO
     */
    @GetMapping("/{reference}")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<TransactionCoreDTO> getByReference(@PathVariable String reference) {
        log.debug("GET /api/v1/transactions/{} - Getting transaction by reference", reference);
        
        TransactionCoreDTO transaction = transactionCoreService.getByReference(reference);
        
        return ResponseEntity.ok(transaction);
    }

    /**
     * Get transaction by ID
     * 
     * GET /api/v1/transactions/id/{id}
     * 
     * Example: GET /api/v1/transactions/id/123
     * 
     * Response: 200 OK with TransactionCoreDTO
     */
    @GetMapping("/id/{id}")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<TransactionCoreDTO> getById(@PathVariable Long id) {
        log.debug("GET /api/v1/transactions/id/{} - Getting transaction by ID", id);
        
        TransactionCoreDTO transaction = transactionCoreService.getById(id);
        
        return ResponseEntity.ok(transaction);
    }

    /**
     * Get wallet transaction history
     * 
     * GET /api/v1/transactions/wallet/{walletId}
     * 
     * Query params:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20, max: 100)
     * 
     * Example: GET /api/v1/transactions/wallet/1?page=0&size=20
     * 
     * Response: 200 OK with Page<TransactionCoreDTO>
     */
    @GetMapping("/wallet/{walletId}")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<Page<TransactionCoreDTO>> getWalletHistory(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("GET /api/v1/transactions/wallet/{} - Getting history page={} size={}", 
                walletId, page, size);
        
        // Limit page size to 100
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionCoreDTO> transactions = transactionCoreService.getWalletHistory(walletId, pageable);
        
        log.debug("Found {} transactions for wallet {}", transactions.getTotalElements(), walletId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get sent transactions for a wallet
     * 
     * GET /api/v1/transactions/wallet/{walletId}/sent
     * 
     * Query params:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20, max: 100)
     * 
     * Response: 200 OK with Page<TransactionCoreDTO>
     */
    @GetMapping("/wallet/{walletId}/sent")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<Page<TransactionCoreDTO>> getSentTransactions(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("GET /api/v1/transactions/wallet/{}/sent - page={} size={}", walletId, page, size);
        
        if (size > 100) size = 100;
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionCoreDTO> transactions = transactionCoreService.getSentTransactions(walletId, pageable);
        
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get received transactions for a wallet
     * 
     * GET /api/v1/transactions/wallet/{walletId}/received
     * 
     * Query params:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20, max: 100)
     * 
     * Response: 200 OK with Page<TransactionCoreDTO>
     */
    @GetMapping("/wallet/{walletId}/received")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<Page<TransactionCoreDTO>> getReceivedTransactions(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("GET /api/v1/transactions/wallet/{}/received - page={} size={}", walletId, page, size);
        
        if (size > 100) size = 100;
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionCoreDTO> transactions = transactionCoreService.getReceivedTransactions(walletId, pageable);
        
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get transactions by type
     * 
     * GET /api/v1/transactions/type/{type}
     * 
     * Types: TRANSFER, DEPOSIT, WITHDRAWAL
     * 
     * Query params:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20, max: 100)
     * 
     * Example: GET /api/v1/transactions/type/TRANSFER?page=0&size=20
     * 
     * Response: 200 OK with Page<TransactionCoreDTO>
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionCoreDTO>> getByType(
            @PathVariable TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("GET /api/v1/transactions/type/{} - page={} size={}", type, page, size);
        
        if (size > 100) size = 100;
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionCoreDTO> transactions = transactionCoreService.getByType(type, pageable);
        
        return ResponseEntity.ok(transactions);
    }

    /* =========================
       LOT 2: NEW ENDPOINTS
       ========================= */

    /**
     * Create Merchant Payment (LOT 2)
     * 
     * POST /api/v1/transactions/merchant-payment
     * 
     * Request params:
     * - senderWalletId: Customer wallet ID
     * - merchantWalletId: Merchant wallet ID
     * - amount: Payment amount
     * - currency: Currency code (EUR, USD, etc.)
     * - description: Payment description
     * 
     * Fees charged:
     * - Merchant fee: 2%
     * - Platform fee: 0.5%
     * - Total: 2.5%
     * 
     * Response: 201 Created with TransactionCoreDTO
     */
    @PostMapping("/merchant-payment")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<TransactionCoreDTO> merchantPayment(
            @RequestParam @NotNull Long senderWalletId,
            @RequestParam @NotNull Long merchantWalletId,
            @RequestParam @NotNull @Positive BigDecimal amount,
            @RequestParam @NotNull String currency,
            @RequestParam(required = false) String description) {
        
        log.info("POST /api/v1/transactions/merchant-payment - Customer wallet {} paying merchant wallet {} amount {}", 
                senderWalletId, merchantWalletId, amount);
        
        TransactionCoreDTO transaction = transactionCoreService.createMerchantPayment(
                senderWalletId, merchantWalletId, amount, currency, description);
        
        log.info("Merchant payment created successfully: {}", transaction.getReference());
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    /**
     * Cancel Transaction (LOT 2)
     * 
     * POST /api/v1/transactions/{reference}/cancel
     * 
     * Only PENDING transactions can be cancelled.
     * 
     * Response: 200 OK with updated TransactionCoreDTO
     */
    @PostMapping("/{reference}/cancel")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<TransactionCoreDTO> cancelTransaction(@PathVariable String reference) {
        log.info("POST /api/v1/transactions/{}/cancel - Cancelling transaction", reference);
        
        TransactionCoreDTO transaction = transactionCoreService.cancelTransaction(reference);
        
        log.info("Transaction cancelled successfully: {}", reference);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Retry Failed Transaction (LOT 2)
     * 
     * POST /api/v1/transactions/{reference}/retry
     * 
     * Only FAILED transactions can be retried.
     * Maximum 3 retry attempts allowed.
     * 
     * Response: 200 OK with updated TransactionCoreDTO
     */
    @PostMapping("/{reference}/retry")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<TransactionCoreDTO> retryTransaction(@PathVariable String reference) {
        log.info("POST /api/v1/transactions/{}/retry - Retrying transaction", reference);
        
        TransactionCoreDTO transaction = transactionCoreService.retryTransaction(reference);
        
        log.info("Transaction retry completed: {}", reference);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Reverse Transaction (LOT 1 - State Management)
     * 
     * POST /api/v1/transactions/{reference}/reverse
     * 
     * Only COMPLETED transactions can be reversed.
     * Creates a new transaction in the opposite direction.
     * ADMIN only.
     * 
     * Response: 200 OK with reversed TransactionCoreDTO
     */
    @PostMapping("/{reference}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionCoreDTO> reverseTransaction(
            @PathVariable String reference,
            @RequestParam(required = false) String reason) {
        log.info("POST /api/v1/transactions/{}/reverse - Reversing transaction. Reason: {}", reference, reason);
        
        TransactionCoreDTO transaction = transactionCoreService.reverseTransaction(reference, reason);
        
        log.info("Transaction reversed successfully: {} -> New reference: {}", reference, transaction.getReference());
        return ResponseEntity.ok(transaction);
    }

    /**
     * Refund Transaction (LOT 1 - State Management)
     * 
     * POST /api/v1/transactions/{reference}/refund
     * 
     * Only COMPLETED transactions can be refunded.
     * Creates a refund transaction returning funds to sender.
     * ADMIN or merchant who received payment can refund.
     * 
     * Response: 200 OK with refund TransactionCoreDTO
     */
    @PostMapping("/{reference}/refund")
    @PreAuthorize("hasRole('ADMIN') or @txSecurity.canRefund(#reference)")
    public ResponseEntity<TransactionCoreDTO> refundTransaction(
            @PathVariable String reference,
            @RequestParam(required = false) String reason) {
        log.info("POST /api/v1/transactions/{}/refund - Refunding transaction. Reason: {}", reference, reason);
        
        TransactionCoreDTO transaction = transactionCoreService.refundTransaction(reference, reason);
        
        log.info("Transaction refunded successfully: {} -> Refund reference: {}", reference, transaction.getReference());
        return ResponseEntity.ok(transaction);
    }

    /* =========================
       LOT 2: BULK & SPLIT PAYMENTS
       ========================= */

    /**
     * Bulk Transfer (LOT 2)
     * 
     * POST /api/v1/transactions/bulk-transfer
     * 
     * Send multiple transfers in one request.
     * MERCHANT and ADMIN only.
     * 
     * Response: 201 Created with Page<TransactionDTO>
     */
    @PostMapping("/bulk-transfer")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public ResponseEntity<Page<TransactionDTO>> bulkTransfer(@Valid @RequestBody BulkTransferRequest request) {
        log.info("POST /api/v1/transactions/bulk-transfer - Processing {} transfers", request.getItems().size());
        
        Page<TransactionDTO> saved = transactionService.createBulkTransfer(request);
        
        log.info("Bulk transfer completed: {} transactions created", saved.getTotalElements());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Split Payment (LOT 2)
     * 
     * POST /api/v1/transactions/split-payment
     * 
     * Split a payment among multiple recipients.
     * REGULAR and MERCHANT can use this.
     * 
     * Response: 201 Created with SplitPaymentResponse
     */
    @PostMapping("/split-payment")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<SplitPaymentResponse> splitPayment(@Valid @RequestBody SplitPaymentRequest request) {
        log.info("POST /api/v1/transactions/split-payment - {} recipients, total {}", 
            request.getRecipients().size(), request.getTotalAmount());
        
        SplitPaymentResponse response = transactionService.createSplitPayment(request);
        
        log.info("Split payment completed: {} transactions created", response.getTransactions().size());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Process Transaction (LOT 1)
     * 
     * POST /api/v1/transactions/{reference}/process
     * 
     * Manually process a pending transaction.
     * Owner or ADMIN only.
     * 
     * Response: 200 OK with updated TransactionDTO
     */
    @PostMapping("/{reference}/process")
    @PreAuthorize("@txSecurity.isOwner(#reference) or hasRole('ADMIN')")
    public ResponseEntity<TransactionDTO> processTransaction(@PathVariable String reference) {
        log.info("POST /api/v1/transactions/{}/process - Processing transaction", reference);
        
        TransactionDTO transaction = transactionService.processTransaction(reference);
        
        log.info("Transaction processed successfully: {}", reference);
        return ResponseEntity.ok(transaction);
    }

    /* =========================
       LOT 4: SEARCH & REPORTING
       ========================= */

    /**
     * Search Transactions (LOT 4)
     * 
     * GET /api/v1/transactions/search
     * 
     * Global search for all transactions.
     * ADMIN only.
     * 
     * Query params:
     * - status: Filter by transaction status
     * - type: Filter by transaction type
     * - currency: Filter by currency
     * - minAmount: Minimum amount
     * - maxAmount: Maximum amount
     * - from: Start date (ISO format)
     * - to: End date (ISO format)
     * - reference: Search by reference
     * - page: Page number (default: 0)
     * - size: Page size (default: 10)
     * 
     * Response: 200 OK with Page<TransactionDTO>
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionDTO>> searchTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String reference,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("GET /api/v1/transactions/search - status={} type={} currency={}", status, type, currency);

        Page<TransactionDTO> transactions = transactionService.searchTransactions(
                status, type, currency, minAmount, maxAmount, from, to, reference, page, size);

        log.debug("Search returned {} results", transactions.getTotalElements());
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get My Transactions (LOT 4)
     * 
     * GET /api/v1/transactions/my-transactions
     * 
     * Get all transactions for the authenticated user.
     * Filtered by current user's wallets.
     * 
     * Query params:
     * - status: Filter by status
     * - type: Filter by type
     * - from: Start date
     * - to: End date
     * - page: Page number (default: 0)
     * - size: Page size (default: 10)
     * 
     * Response: 200 OK with Page<TransactionDTO>
     */
    @GetMapping("/my-transactions")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<Page<TransactionDTO>> getMyTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("GET /api/v1/transactions/my-transactions - status={} type={}", status, type);

        Page<TransactionDTO> transactions = transactionService.getMyTransactions(
                status, type, from, to, page, size);
        
        log.debug("Found {} transactions for current user", transactions.getTotalElements());
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get My Sent Transactions (LOT 4)
     * 
     * GET /api/v1/transactions/my-transactions/sent
     * 
     * Get all sent transactions for the authenticated user.
     * 
     * Response: 200 OK with Page<TransactionDTO>
     */
    @GetMapping("/my-transactions/sent")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<Page<TransactionDTO>> getMySentTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("GET /api/v1/transactions/my-transactions/sent - status={}", status);

        Page<TransactionDTO> transactions = transactionService.getMySentTransactions(
                status, page, size);
        
        log.debug("Found {} sent transactions", transactions.getTotalElements());
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get My Received Transactions (LOT 4)
     * 
     * GET /api/v1/transactions/my-transactions/received
     * 
     * Get all received transactions for the authenticated user.
     * 
     * Response: 200 OK with Page<TransactionDTO>
     */
    @GetMapping("/my-transactions/received")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<Page<TransactionDTO>> getMyReceivedTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("GET /api/v1/transactions/my-transactions/received - status={}", status);

        Page<TransactionDTO> transactions = transactionService.getMyReceivedTransactions(
                status, page, size);
        
        log.debug("Found {} received transactions", transactions.getTotalElements());
        return ResponseEntity.ok(transactions);
    }
}
