package com.zaphira.transaction.controller;

import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.requests.CreateTransactionRequest;
import com.zaphira.transaction.service.TransactionServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionServiceImpl transactionService;

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionDTO saved = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Traiter une transaction
     */
    @PostMapping("/{transactionReference}/process")
    public ResponseEntity<TransactionDTO> processTransaction(
            @PathVariable String transactionReference) {
        log.info("Processing transaction: {}", transactionReference);
        TransactionDTO transaction = transactionService.processTransaction(transactionReference);
        return ResponseEntity.ok(transaction);
    }

//    /**
//     * Initier un transfert
//     */
//    @PostMapping("/transfer")
//    public ResponseEntity<TransactionDTO> initiateTransfer(
//            @Valid @RequestBody TransferRequest request) {
//        log.info("Initiating transfer from {} to {}",
//                request.getSenderWalletNumber(), request.getReceiverWalletNumber());
//        TransactionDTO transaction = transactionService.initiateTransfer(request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
//    }
//
//    /**
//     * Initier un retrait
//     */
//    @PostMapping("/withdrawal")
//    public ResponseEntity<TransactionDTO> initiateWithdrawal(
//            @Valid @RequestBody WithdrawalRequest request) {
//        log.info("Initiating withdrawal from {}", request.getWalletNumber());
//        TransactionDTO transaction = transactionService.initiateWithdrawal(request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
//    }
//
//    /**
//     * Initier un dépôt
//     */
//    @PostMapping("/deposit")
//    public ResponseEntity<TransactionDTO> initiateDeposit(
//            @Valid @RequestBody DepositRequest request) {
//        log.info("Initiating deposit to {}", request.getWalletNumber());
//        TransactionDTO transaction = transactionService.initiateDeposit(request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    /**
     * Obtenir une transaction par référence
     */
//    @GetMapping("/{transactionReference}")
//    public ResponseEntity<TransactionDTO> getTransaction(
//            @PathVariable String transactionReference) {
//        TransactionDTO transaction = transactionService.getTransaction(transactionReference);
//        return ResponseEntity.ok(transaction);
//    }
//
//
//    /**
//     * Obtenir les transactions d'un utilisateur
//     */
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<Page<TransactionDTO>> getUserTransactions(
//            @PathVariable Long userId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//        Page<TransactionDTO> transactions = transactionService.getUserTransactions(userId, page, size);
//        return ResponseEntity.ok(transactions);
//    }

    /**
     * Obtenir les transactions d'un wallet
     */
//    @GetMapping("/wallet/{walletNumber}")
//    public ResponseEntity<Page<TransactionDTO>> getWalletTransactions(
//            @PathVariable String walletNumber,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//        Page<TransactionDTO> transactions = transactionService.getWalletTransactions(walletNumber, page, size);
//        return ResponseEntity.ok(transactions);
//    }

    /**
     * Rechercher des transactions
     */
//    @PostMapping("/search")
//    public ResponseEntity<Page<TransactionDTO>> searchTransactions(
//            @RequestBody TransactionSearchCriteria criteria) {
//        Page<TransactionDTO> transactions = transactionService.searchTransactions(criteria);
//        return ResponseEntity.ok(transactions);
//    }

    /**
//     * Obtenir le résumé des transactions
//     */
//    @GetMapping("/summary/user/{userId}")
//    public ResponseEntity<TransactionSummaryDTO> getTransactionSummary(
//            @PathVariable Long userId,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
//        TransactionSummaryDTO summary = transactionService.getTransactionSummary(userId, startDate, endDate);
//        return ResponseEntity.ok(summary);
//    }
//

//


    /**
     * Compléter une transaction
//     */
//    @PostMapping("/{transactionReference}/complete")
//    public ResponseEntity<TransactionDTO> completeTransaction(
//            @PathVariable String transactionReference) {
//        log.info("Completing transaction: {}", transactionReference);
//        TransactionDTO transaction = transactionService.completeTransaction(transactionReference);
//        return ResponseEntity.ok(transaction);
//    }
//
//    /**
//     * Annuler une transaction
//     */
//    @PostMapping("/{transactionReference}/cancel")
//    public ResponseEntity<TransactionDTO> cancelTransaction(
//            @PathVariable String transactionReference,
//            @RequestParam String reason) {
//        log.info("Cancelling transaction: {}", transactionReference);
//        TransactionDTO transaction = transactionService.cancelTransaction(transactionReference, reason);
//        return ResponseEntity.ok(transaction);
//    }

//
//    /**
//     * Inverser une transaction
//     */
//    @PostMapping("/{transactionReference}/reverse")
//    public ResponseEntity<TransactionDTO> reverseTransaction(
//            @PathVariable String transactionReference,
//            @RequestParam String reason) {
//        log.info("Reversing transaction: {}", transactionReference);
//        TransactionDTO transaction = transactionService.reverseTransaction(transactionReference, reason);
//        return ResponseEntity.ok(transaction);
//    }
//
//    /**
//     * Obtenir les transactions en attente
//     */
//    @GetMapping("/pending")
//    public ResponseEntity<List<TransactionDTO>> getPendingTransactions() {
//        List<TransactionDTO> transactions = transactionService.getPendingTransactions();
//        return ResponseEntity.ok(transactions);
//    }
//
//    /**
//     * Réessayer une transaction échouée
//     */
//    @PostMapping("/{transactionReference}/retry")
//    public ResponseEntity<Void> retryFailedTransaction(
//            @PathVariable String transactionReference) {
//        log.info("Retrying failed transaction: {}", transactionReference);
//        transactionService.retryFailedTransaction(transactionReference);
//        return ResponseEntity.accepted().build();
//    }
//
//    /**
//     * Vérifier si une référence existe
//     */
//    @GetMapping("/exists/{transactionReference}")
//    public ResponseEntity<Boolean> existsByReference(
//            @PathVariable String transactionReference) {
//        boolean exists = transactionService.existsByReference(transactionReference);
//        return ResponseEntity.ok(exists);
//    }



//    @GetMapping
//    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
//        return ResponseEntity.ok(transactionService.getAllTransactions());
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable Long id) {
//        Transaction tx = transactionService.getTransaction(id);
//        return ResponseEntity.ok(tx);
//    }
//
//    @GetMapping("/{id}/history")
//    public ResponseEntity<List<TransactionStateHistory>> getTransactionHistory(@PathVariable Long id) {
//        return ResponseEntity.ok(transactionService.getTransactionHistory(id));
//    }
//
//    @GetMapping("/{id}/authorization")
//    public ResponseEntity<AuthorizationInfoResponse> getAuthorizationInfo(@PathVariable Long id) {
//        AuthorizationInfoResponse response = transactionService.getAuthorizationInfo(id);
//        if (response == null) {
//            return ResponseEntity.notFound().build();
//        }
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/{id}/authorize")
//    public ResponseEntity<TransactionDTO> authorizeTransaction(@PathVariable Long id,
//                                                            @Valid @RequestBody AuthorizationValidationRequest request) {
//        Transaction tx = transactionService.authorizeTransaction(id, request);
//        return ResponseEntity.ok(tx);
//    }
//
//    @PutMapping("/{id}/status")
//    public ResponseEntity<TransactionDTO> updateStatus(@PathVariable Long id,
//                                                    @Valid @RequestBody UpdateStatusRequest request) {
//        TransactionDTO updated = transactionService.updateTransactionStatus(id, request);
//        return ResponseEntity.ok(updated);
//    }
//
//    @PutMapping("/{id}/cancel")
//    public ResponseEntity<TransactionDTO> cancelTransaction(@PathVariable Long id,
//                                                         @Valid @RequestBody UpdateStatusRequest request) {
//        TransactionDTO cancelled = transactionService.cancelTransaction(id, request);
//        return ResponseEntity.ok(cancelled);
//    }



