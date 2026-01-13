package com.zaphira.transaction.controller;

import com.zaphira.transaction.dto.requests.ScheduledTransactionRequest;
import com.zaphira.transaction.dto.response.ScheduledTransactionResponse;
import com.zaphira.transaction.service.ScheduledTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/scheduled")
public class ScheduledTransactionController {

    private final ScheduledTransactionService scheduledTransactionService;

    public ScheduledTransactionController(ScheduledTransactionService scheduledTransactionService) {
        this.scheduledTransactionService = scheduledTransactionService;
    }

    @PostMapping
    public ResponseEntity<ScheduledTransactionResponse> create(@Valid @RequestBody ScheduledTransactionRequest request) {
        ScheduledTransactionResponse response = scheduledTransactionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ScheduledTransactionResponse>> findAll() {
        return ResponseEntity.ok(scheduledTransactionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledTransactionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduledTransactionService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id,
                                       @RequestParam String cancelledBy,
                                       @RequestParam(required = false) String reason) {
        scheduledTransactionService.cancel(id, cancelledBy, reason);
        return ResponseEntity.noContent().build();
    }
}


