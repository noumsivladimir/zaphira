package com.zaphira.transaction.controller;

import com.zaphira.transaction.dto.AuthorizationInfoResponse;
import com.zaphira.transaction.dto.AuthorizationValidationRequest;
import com.zaphira.transaction.dto.TransactionRequest;
import com.zaphira.transaction.dto.UpdateStatusRequest;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.TransactionStateHistory;
import com.zaphira.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
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
}


