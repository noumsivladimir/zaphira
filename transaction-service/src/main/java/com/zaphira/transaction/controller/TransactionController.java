package com.zaphira.transaction.controller;

import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.requests.CreateTransactionRequest;
import com.zaphira.transaction.dto.requests.TransferRequest;
import com.zaphira.transaction.dto.requests.PaymentRequest;
import com.zaphira.transaction.dto.requests.BulkTransferRequest;
import com.zaphira.transaction.dto.requests.SplitPaymentRequest;
import com.zaphira.transaction.dto.response.SplitPaymentResponse;
import com.zaphira.transaction.service.TransactionServiceImpl;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @deprecated This controller is deprecated. All endpoints have been migrated to TransactionCoreController (/api/v1/transactions).
 * Please use TransactionCoreController for all new integrations.
 * This controller will be removed in a future version.
 */
@Deprecated(since = "2026-02-04", forRemoval = true)
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionServiceImpl transactionService;

    public TransactionController(TransactionServiceImpl transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionDTO saved = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // LOT 1: Initiate Transfer - REGULAR + MERCHANT
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferRequest request) {
        TransactionDTO saved = transactionService.createTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // LOT 1: Deposit - REGULAR + MERCHANT
    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<TransactionDTO> deposit(@Valid @RequestBody TransferRequest request) {
        TransactionDTO saved = transactionService.createDeposit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // LOT 1: Withdrawal - REGULAR + MERCHANT
    @PostMapping("/withdrawal")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<TransactionDTO> withdrawal(@Valid @RequestBody TransferRequest request) {
        TransactionDTO saved = transactionService.createWithdrawal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // LOT 1: Merchant Payment - REGULAR only (paying to merchant)
    @PostMapping("/merchant-payment")
    @PreAuthorize("hasRole('REGULAR')")
    public ResponseEntity<TransactionDTO> merchantPayment(@Valid @RequestBody PaymentRequest request) {
        TransactionDTO saved = transactionService.createMerchantPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // LOT 2: Bulk Transfer - MERCHANT + ADMIN
    @PostMapping("/bulk-transfer")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public ResponseEntity<Page<TransactionDTO>> bulkTransfer(@Valid @RequestBody BulkTransferRequest request) {
        Page<TransactionDTO> saved = transactionService.createBulkTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // LOT 2: Split Payment - REGULAR + MERCHANT (split a payment among multiple recipients)
    @PostMapping("/split-payment")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<SplitPaymentResponse> splitPayment(@Valid @RequestBody SplitPaymentRequest request) {
        log.info("Split payment request: {} recipients, total {}", 
            request.getRecipients().size(), request.getTotalAmount());
        SplitPaymentResponse response = transactionService.createSplitPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * LOT 1: Process Transaction - Owner only
     */
    @PostMapping("/{transactionReference}/process")
    @PreAuthorize("@txSecurity.isOwner(#transactionReference)")
    public ResponseEntity<TransactionDTO> processTransaction(
            @PathVariable String transactionReference) {
        log.info("Processing transaction: {}", transactionReference);
        TransactionDTO transaction = transactionService.processTransaction(transactionReference);
        return ResponseEntity.ok(transaction);
    }

    // LOT 1: Cancel Transaction - Owner only
    @PostMapping("/{transactionReference}/cancel")
    @PreAuthorize("@txSecurity.isOwner(#transactionReference)")
    public ResponseEntity<TransactionDTO> cancelTransaction(
            @PathVariable String transactionReference,
            @RequestParam String reason) {
        log.info("Cancelling transaction: {}", transactionReference);
        TransactionDTO transaction = transactionService.cancelTransaction(transactionReference, reason);
        return ResponseEntity.ok(transaction);
    }

    // LOT 1: Get Transaction Details - Participant or ADMIN
    @GetMapping("/{transactionReference}")
    @PreAuthorize("@txSecurity.canView(#transactionReference)")
    public ResponseEntity<TransactionDTO> getTransaction(
            @PathVariable String transactionReference) {
        TransactionDTO transaction = transactionService.getTransaction(transactionReference);
        return ResponseEntity.ok(transaction);
    }

    // Get Transaction by ID - ADMIN only (internal ID exposure)
    @GetMapping("/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable Long id) {
        TransactionDTO transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }

    // LOT 1: Get Wallet Transactions - Authenticated users only (service will filter by userId)
    @GetMapping("/wallet/{walletNumber}")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<Page<TransactionDTO>> getWalletTransactions(
            @PathVariable String walletNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TransactionDTO> transactions = transactionService.getWalletTransactions(walletNumber, page, size);
        return ResponseEntity.ok(transactions);
    }

    // LOT 4: Search Transactions - ADMIN only (global search)
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

        Page<TransactionDTO> transactions = transactionService.searchTransactions(
                status,
                type,
                currency,
                minAmount,
                maxAmount,
                from,
                to,
                reference,
                page,
                size);

        return ResponseEntity.ok(transactions);
    }

    // LOT 5: Reverse Transaction - ADMIN only
    @PostMapping("/{transactionReference}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionDTO> reverseTransaction(
            @PathVariable String transactionReference,
            @RequestParam(required = false) String reason) {
        TransactionDTO tx = transactionService.reverseTransaction(transactionReference, reason);
        return ResponseEntity.ok(tx);
    }

    // LOT 5: Refund Transaction - ADMIN or Merchant who received payment
    @PostMapping("/{transactionReference}/refund")
    @PreAuthorize("@txSecurity.canRefund(#transactionReference)")
    public ResponseEntity<TransactionDTO> refundTransaction(
            @PathVariable String transactionReference,
            @RequestParam(required = false) String reason) {
        TransactionDTO tx = transactionService.refundTransaction(transactionReference, reason);
        return ResponseEntity.ok(tx);
    }

    // LOT 1: Retry Failed Transaction - Owner only
    @PostMapping("/{transactionReference}/retry")
    @PreAuthorize("@txSecurity.isOwner(#transactionReference)")
    public ResponseEntity<TransactionDTO> retryTransaction(
            @PathVariable String transactionReference) {
        TransactionDTO tx = transactionService.retryFailedTransaction(transactionReference);
        return ResponseEntity.ok(tx);
    }

    // ========== LOT 4: User-scoped Search & Reports ==========

    // LOT 4: Get My Transactions - REGULAR/MERCHANT (filtered by current userId)
    @GetMapping("/my-transactions")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<Page<TransactionDTO>> getMyTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<TransactionDTO> transactions = transactionService.getMyTransactions(
                status, type, from, to, page, size);
        
        return ResponseEntity.ok(transactions);
    }

    // LOT 4: Get Sent Transactions - User's sent transactions only
    @GetMapping("/my-transactions/sent")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<Page<TransactionDTO>> getMySentTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<TransactionDTO> transactions = transactionService.getMySentTransactions(
                status, page, size);
        
        return ResponseEntity.ok(transactions);
    }

    // LOT 4: Get Received Transactions - User's received transactions only
    @GetMapping("/my-transactions/received")
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT')")
    public ResponseEntity<Page<TransactionDTO>> getMyReceivedTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<TransactionDTO> transactions = transactionService.getMyReceivedTransactions(
                status, page, size);
        
        return ResponseEntity.ok(transactions);
    }
}

