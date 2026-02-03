package com.zaphira.transaction.controller;

import com.zaphira.transaction.dto.requests.ScheduledTransactionRequest;
import com.zaphira.transaction.dto.response.ScheduledTransactionResponse;
import com.zaphira.transaction.service.ScheduledTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/scheduled")
@PreAuthorize("hasAnyRole('REGULAR','MERCHANT','ADMIN')")
public class ScheduledTransactionController {

    private final ScheduledTransactionService scheduledTransactionService;

    public ScheduledTransactionController(ScheduledTransactionService scheduledTransactionService) {
        this.scheduledTransactionService = scheduledTransactionService;
    }

    // LOT 3: Create Scheduled Transaction - REGULAR/MERCHANT can schedule their own transactions
    @PostMapping
    public ResponseEntity<ScheduledTransactionResponse> create(@Valid @RequestBody ScheduledTransactionRequest request) {
        ScheduledTransactionResponse response = scheduledTransactionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // LOT 3: List My Scheduled Transactions - User sees only their own
    @GetMapping
    public ResponseEntity<List<ScheduledTransactionResponse>> findAll() {
        return ResponseEntity.ok(scheduledTransactionService.findAll());
    }

    // LOT 3: Get Scheduled Transaction Details - Owner or ADMIN
    @GetMapping("/{id}")
    @PreAuthorize("@scheduledTxSecurity.isOwner(#id) or hasRole('ADMIN')")
    public ResponseEntity<ScheduledTransactionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduledTransactionService.findById(id));
    }

    // LOT 3: Cancel Scheduled Transaction - Owner only (if not yet executed)
    @DeleteMapping("/{id}")
    @PreAuthorize("@scheduledTxSecurity.isOwner(#id)")
    public ResponseEntity<Void> cancel(@PathVariable Long id,
                                       @RequestParam String cancelledBy,
                                       @RequestParam(required = false) String reason) {
        scheduledTransactionService.cancel(id, cancelledBy, reason);
        return ResponseEntity.noContent().build();
    }
}


