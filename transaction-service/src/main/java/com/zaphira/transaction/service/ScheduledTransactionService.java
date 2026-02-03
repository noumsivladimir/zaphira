package com.zaphira.transaction.service;

import com.zaphira.transaction.dto.requests.CreateTransactionRequest;
import com.zaphira.transaction.dto.requests.ScheduledTransactionRequest;
import com.zaphira.transaction.dto.response.ScheduledTransactionResponse;
import com.zaphira.transaction.model.ScheduledTransaction;
import com.zaphira.transaction.model.enums.ScheduledTransactionStatus;
import com.zaphira.transaction.model.enums.TransactionCategory;
import com.zaphira.transaction.repository.ScheduledTransactionRepository;
import com.zaphira.transaction.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduledTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTransactionService.class);

    private final ScheduledTransactionRepository repository;
    private final TransactionServiceImpl transactionServiceImpl;

    public ScheduledTransactionService(ScheduledTransactionRepository repository,
                                       TransactionServiceImpl transactionServiceImpl) {
        this.repository = repository;
        this.transactionServiceImpl = transactionServiceImpl;
    }

    @Transactional
    public ScheduledTransactionResponse create(@Valid ScheduledTransactionRequest request) {
        AuthenticatedUser user = requireUser();

        String requester = request.getRequestedBy() != null ? request.getRequestedBy() : String.valueOf(user.getId());
        String rolesCsv = user.getRoles() == null ? null : String.join(",", user.getRoles());

        ScheduledTransaction scheduled = ScheduledTransaction.builder()
                .senderWalletNumber(request.getSenderWalletNumber())
                .receiverWalletNumber(request.getReceiverWalletNumber())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .type(request.getType())
                .channel(request.getChannel())
                .description(request.getDescription())
                .requestedBy(requester)
                .requesterUserId(user.getId())
                .requesterRoles(rolesCsv)
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

    @Scheduled(fixedDelayString = "60000")
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

        var previousAuth = SecurityContextHolder.getContext().getAuthentication();
        try {
            if (scheduled.getRequesterUserId() != null) {
                setAuthenticationForScheduled(scheduled);
            }

            CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
                    .senderWalletNumber(scheduled.getSenderWalletNumber())
                    .receiverWalletNumber(scheduled.getReceiverWalletNumber())
                    .amount(scheduled.getAmount())
                    .currency(scheduled.getCurrency())
                    .category(TransactionCategory.WALLET_TO_WALLET)
                    .type(scheduled.getType())
                    .channel(scheduled.getChannel())
                    .description(scheduled.getDescription())
                    .build();

            var txDto = transactionServiceImpl.createTransaction(txRequest);
            scheduled.setExecutedTransactionId(txDto.getId());
            scheduled.setStatus(ScheduledTransactionStatus.COMPLETED);
            scheduled.setLastError(null);
        } catch (Exception ex) {
            scheduled.setStatus(ScheduledTransactionStatus.FAILED);
            scheduled.setLastError(ex.getMessage());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuth);
        }
        repository.save(scheduled);
    }

    private void setAuthenticationForScheduled(ScheduledTransaction scheduled) {
        List<SimpleGrantedAuthority> authorities = scheduled.getRequesterRoles() == null ? List.of()
                : java.util.Arrays.stream(scheduled.getRequesterRoles().split(","))
                .filter(r -> !r.isBlank())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        AuthenticatedUser principal = new AuthenticatedUser(
                scheduled.getRequesterUserId(),
                scheduled.getRequestedBy(),
                scheduled.getRequesterRoles() == null ? null : java.util.Arrays.asList(scheduled.getRequesterRoles().split(","))
        );

        var token = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private AuthenticatedUser requireUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser au) {
            return au;
        }
        throw new IllegalStateException("Unauthenticated request");
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


