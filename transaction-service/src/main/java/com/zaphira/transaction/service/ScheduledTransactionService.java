package com.zaphira.transaction.service;

import com.zaphira.transaction.dto.ScheduledTransactionRequest;
import com.zaphira.transaction.dto.ScheduledTransactionResponse;
import com.zaphira.transaction.dto.TransactionRequest;
import com.zaphira.transaction.model.ScheduledTransaction;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.enums.ScheduledTransactionStatus;
import com.zaphira.transaction.repository.ScheduledTransactionRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduledTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTransactionService.class);

    private final ScheduledTransactionRepository repository;
    private final TransactionService transactionService;

    public ScheduledTransactionService(ScheduledTransactionRepository repository,
                                       TransactionService transactionService) {
        this.repository = repository;
        this.transactionService = transactionService;
    }

    @Transactional
    public ScheduledTransactionResponse create(@Valid ScheduledTransactionRequest request) {
        ScheduledTransaction scheduled = ScheduledTransaction.builder()
                .senderWalletNumber(request.getSenderWalletNumber())
                .receiverWalletNumber(request.getReceiverWalletNumber())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .type(request.getType())
                .channel(request.getChannel())
                .description(request.getDescription())
                .requestedBy(request.getRequestedBy())
                .scheduledFor(request.getScheduledFor())
                .status(ScheduledTransactionStatus.PENDING)
                .build();
        ScheduledTransaction saved = repository.save(scheduled);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ScheduledTransactionResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScheduledTransactionResponse findById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ScheduledTransactionNotFoundException(id));
    }

    @Transactional
    public void cancel(Long id, String cancelledBy, String reason) {
        ScheduledTransaction scheduled = repository.findById(id)
                .orElseThrow(() -> new ScheduledTransactionNotFoundException(id));
        if (scheduled.getStatus() == ScheduledTransactionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed scheduled transaction");
        }
        scheduled.setStatus(ScheduledTransactionStatus.CANCELLED);
        scheduled.setLastError("Cancelled by " + cancelledBy + (reason != null ? (": " + reason) : ""));
        repository.save(scheduled);
    }

    @Scheduled(fixedDelayString = "15000")
    @Transactional
    public void processDueSchedules() {
        List<ScheduledTransaction> due = repository.findDue(LocalDateTime.now());
        if (due.isEmpty()) {
            return;
        }
        log.info("Processing {} due scheduled transactions", due.size());
        for (ScheduledTransaction scheduled : due) {
            try {
                executeScheduledTransaction(scheduled);
            } catch (Exception ex) {
                log.error("Failed to execute scheduled transaction id={}", scheduled.getId(), ex);
            }
        }
    }

    @Transactional
    public void executeScheduledTransaction(ScheduledTransaction scheduled) {
        if (scheduled.getStatus() != ScheduledTransactionStatus.PENDING) {
            return;
        }
        scheduled.setStatus(ScheduledTransactionStatus.RUNNING);
        scheduled.setLastExecutionAt(LocalDateTime.now());
        repository.save(scheduled);

        try {
            TransactionRequest request = new TransactionRequest();
            request.setSenderWalletNumber(scheduled.getSenderWalletNumber());
            request.setReceiverWalletNumber(scheduled.getReceiverWalletNumber());
            request.setAmount(scheduled.getAmount());
            request.setCurrency(scheduled.getCurrency());
            request.setType(scheduled.getType());
            request.setChannel(scheduled.getChannel());
            request.setDescription(scheduled.getDescription());
            request.setRequestedBy(scheduled.getRequestedBy());
            request.setProcessInstantly(true);

            Transaction tx = transactionService.createTransaction(request);
            scheduled.setExecutedTransactionId(tx.getId());
            scheduled.setStatus(ScheduledTransactionStatus.COMPLETED);
            scheduled.setLastError(null);
        } catch (Exception ex) {
            scheduled.setStatus(ScheduledTransactionStatus.FAILED);
            scheduled.setLastError(ex.getMessage());
        }
        repository.save(scheduled);
    }

    private ScheduledTransactionResponse toResponse(ScheduledTransaction scheduled) {
        return ScheduledTransactionResponse.builder()
                .id(scheduled.getId())
                .senderWalletNumber(scheduled.getSenderWalletNumber())
                .receiverWalletNumber(scheduled.getReceiverWalletNumber())
                .amount(scheduled.getAmount())
                .currency(scheduled.getCurrency())
                .type(scheduled.getType())
                .channel(scheduled.getChannel())
                .description(scheduled.getDescription())
                .requestedBy(scheduled.getRequestedBy())
                .scheduledFor(scheduled.getScheduledFor())
                .status(scheduled.getStatus())
                .lastError(scheduled.getLastError())
                .executedTransactionId(scheduled.getExecutedTransactionId())
                .createdAt(scheduled.getCreatedAt())
                .lastExecutionAt(scheduled.getLastExecutionAt())
                .build();
    }
}


