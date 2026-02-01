package com.zaphira.transaction.service;

import com.zaphira.common.dto.WalletSummaryDTO;
import com.zaphira.common.dto.request.BalanceOperationRequest;
import com.zaphira.common.model.enums.Currency;
import com.zaphira.common.model.enums.PermissionType;
import com.zaphira.transaction.client.WalletServiceClient;
import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.TransactionSummaryDTO;
import com.zaphira.transaction.dto.requests.CreateTransactionRequest;
import com.zaphira.transaction.event.TransactionEventPublisher;
import com.zaphira.transaction.exception.TransactionExceptions;
import com.zaphira.transaction.mapper.TransactionMapper;
import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.model.entities.TransactionStateHistory;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.repository.TransactionRepository;
import com.zaphira.transaction.repository.TransactionStateHistoryRepository;
import com.zaphira.transaction.util.TransactionReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionStateHistoryRepository historyRepository;
    private final WalletServiceClient walletServiceClient;
    private final TransactionEventPublisher eventPublisher;
    private final TransactionMapper transactionMapper;
    private final TransactionReferenceGenerator referenceGenerator;

    /* =========================================================
       CREATE
       ========================================================= */

    @Override
    public TransactionDTO createTransaction(CreateTransactionRequest request) {

//        WalletIdResponse senderWalletIdResponse = walletServiceClient.getWalletIdByWalletNumber(request.getSenderWalletNumber());
//        WalletIdResponse  receiverWalletIdResponse = walletServiceClient.getWalletIdByWalletNumber(request.getReceiverWalletNumber());


        WalletSummaryDTO senderWalletIdSummary = walletServiceClient.getWalletSummaryByWalletNumber(request.getSenderWalletNumber());
        WalletSummaryDTO receiverWalletSummary = walletServiceClient.getWalletSummaryByWalletNumber(request.getReceiverWalletNumber());

        Long senderWalletIds = senderWalletIdSummary.getWalletId();
        Long receiverWalletIds = receiverWalletSummary.getWalletId();

        log.info("Creating transaction for wallet {}, receiver wallet {}", senderWalletIds, receiverWalletIds);

        validateRequest(request);

        if(request.getType() == TransactionType.TRANSFER){

            Transaction transaction = Transaction.builder()

                    .reference(referenceGenerator.generate())
                    .senderWalletId(senderWalletIds)
                    .senderWalletNumber(senderWalletIdSummary.getWalletNumber())
                    .receiverWalletNumber(receiverWalletSummary.getWalletNumber())
                    .receiverWalletId(receiverWalletIds)
                    .type(TransactionType.TRANSFER)
                    .category(request.getCategory())
                    .channel(request.getChannel())
                    .status(TransactionStatus.PENDING)
                    .amount(request.getAmount())
                    .feeAmount(BigDecimal.ZERO)
                    .currency(Currency.XAF)
                    .description(request.getDescription())
                    .build();

            Transaction saved = transactionRepository.save(transaction);

            log.info("Created transaction with id {}", transaction.getId());
            changeStatus(transaction, TransactionStatus.PENDING, "Transaction créée");

            eventPublisher.publishTransactionCreated(transaction);

            return processTransaction(transaction.getReference());
        } else if (request.getType() == TransactionType.DEPOSIT) {

            Transaction transaction = Transaction.builder()

                    .reference(referenceGenerator.generate())
                    .senderWalletId(senderWalletIds)
                    .senderWalletNumber(senderWalletIdSummary.getWalletNumber())
                    .receiverWalletNumber(receiverWalletSummary.getWalletNumber())
                    .receiverWalletId(receiverWalletIds)
                    .type(TransactionType.DEPOSIT)
                    .category(request.getCategory())
                    .channel(request.getChannel())
                    .status(TransactionStatus.PENDING)
                    .amount(request.getAmount())
                    .feeAmount(BigDecimal.ZERO)
                    .currency(Currency.XAF)
                    .description(request.getDescription())
                    .build();

            Transaction saved = transactionRepository.save(transaction);

            log.info("Created transaction with id {}", transaction.getId());
            changeStatus(transaction, TransactionStatus.PENDING, "Transaction créée");

            eventPublisher.publishTransactionCreated(transaction);

            return processTransaction(transaction.getReference());
        } else if (request.getType() == TransactionType.WITHDRAWAL) {
            Transaction transaction = Transaction.builder()

                    .reference(referenceGenerator.generate())
                    .senderWalletId(senderWalletIds)
                    .senderWalletNumber(senderWalletIdSummary.getWalletNumber())
                    .receiverWalletNumber(receiverWalletSummary.getWalletNumber())
                    .receiverWalletId(receiverWalletIds)
                    .type(TransactionType.WITHDRAWAL)
                    .category(request.getCategory())
                    .channel(request.getChannel())
                    .status(TransactionStatus.PENDING)
                    .amount(request.getAmount())
                    .feeAmount(BigDecimal.ZERO)
                    .currency(Currency.XAF)
                    .description(request.getDescription())
                    .build();

            Transaction saved = transactionRepository.save(transaction);

            log.info("Created transaction with id {}", transaction.getId());
            changeStatus(transaction, TransactionStatus.PENDING, "Transaction créée");

            eventPublisher.publishTransactionCreated(transaction);

            return processTransaction(transaction.getReference());

        }

        return null;


    }

    @Override
    public Boolean hasPermissionTo(PermissionType permissionType, @PathVariable("walletId") Long walletId) {

        List<PermissionType> permissionTypes = walletServiceClient.getPermissionTypes(walletId);

        for (PermissionType type : permissionTypes) {
            if (type.equals(permissionType)) {
                return true;
            }
        }

        return false;
    }

    /* =========================================================
       PROCESS
       ========================================================= */

    @Override
    public TransactionDTO processTransaction(String reference) {

        Transaction tx = findByReferenceForUpdate(reference);

        if (tx.getStatus() != TransactionStatus.PENDING) {
            throw new TransactionExceptions.InvalidTransactionStatusException(
                    "Invalid status: " + tx.getStatus());
        }

        try {
            changeStatus(tx, TransactionStatus.PROCESSING, "Traitement démarré");

            if (requiresDebit(tx)) {
                blockFunds(tx);
            }



            log.info("Processing transaction {}", tx.getId());

            return completeTransaction(reference);

        } catch (Exception e) {
            log.error("Processing failed", e);
            return failTransaction(reference, e.getMessage());
        }
    }

    @Override
    public TransactionDTO completeTransaction(String reference) {

        Transaction tx = findByReferenceForUpdate(reference);

        if (tx.getStatus() != TransactionStatus.PROCESSING) {
            throw new TransactionExceptions.InvalidTransactionStatusException(
                    "Cannot complete from status " + tx.getStatus());
        }

        executeWalletOperations(tx);

        changeStatus(tx, TransactionStatus.COMPLETED, "Transaction complétée");

       Transaction saved = transactionRepository.save(tx);
        eventPublisher.publishTransactionCompleted(tx);
//
//        return TransactionDTO.builder()
//                .id(saved.getId())
//                .completedAt(saved.getCreatedAt())
//                .build();

        return transactionMapper.toDTO(tx);
    }

    /* =========================================================
       FAIL / CANCEL
       ========================================================= */

    @Override
    public TransactionDTO failTransaction(String reference, String reason) {

        Transaction tx = findByReferenceForUpdate(reference);

        if (tx.getStatus() == TransactionStatus.PROCESSING) {
            unblockFunds(tx);
        }

        tx.setFailureReason(reason);
        changeStatus(tx, TransactionStatus.FAILED, reason);

        transactionRepository.save(tx);
        eventPublisher.publishTransactionFailed(tx);

        return transactionMapper.toDTO(tx);
    }

    @Override
    public TransactionDTO cancelTransaction(String reference, String reason) {

        Transaction tx = findByReferenceForUpdate(reference);

        if (!canCancel(tx)) {
            throw new TransactionExceptions.InvalidTransactionStatusException(
                    "Cannot cancel transaction in status " + tx.getStatus());
        }

        if (tx.getStatus() == TransactionStatus.PROCESSING) {
            unblockFunds(tx);
        }

        changeStatus(tx, TransactionStatus.CANCELLED, reason);

        transactionRepository.save(tx);
        eventPublisher.publishTransactionCancelled(tx);

        return transactionMapper.toDTO(tx);
    }

    /* =========================================================
       READ
       ========================================================= */

    @Override
    public TransactionDTO getTransaction(String reference) {
        return transactionMapper.toDTO(findByReference(reference));
    }

    @Override
    public TransactionDTO getTransactionById(Long id) {
        return transactionMapper.toDTO(
                transactionRepository.findById(id)
                        .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException("TX not found"))
        );
    }

    @Override
    public Page<TransactionDTO> getUserTransactions(Long userId, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
//        return transactionRepository.findByUserId(userId, pageable)
//                .map(transactionMapper::toDTO);

        return null;
    }

    /* =========================================================
       SUMMARY
       ========================================================= */

    @Override
    public TransactionSummaryDTO getTransactionSummary(Long userId,
                                                       LocalDate startDate,
                                                       LocalDate endDate) {

//        List<Transaction> transactions = transactionRepository.findByUserIdAndCreatedAtBetween(
//                userId,
//                startDate.atStartOfDay(),
//                endDate.atTime(23, 59, 59)
//        );
//
//        BigDecimal total = sum(transactions, TransactionType.CREDIT, TransactionType.DEPOSIT);
//        BigDecimal debit = sum(transactions, TransactionType.DEBIT, TransactionType.TRANSFER, TransactionType.WITHDRAWAL);
//        BigDecimal fees = transactions.stream()
//                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
//                .map(t -> t.getFeeAmount() == null ? BigDecimal.ZERO : t.getFeeAmount())
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        return TransactionSummaryDTO.builder()
//                .userId(userId)
//                .totalAmount(total)
//                .totalDebits(debit)
//                .totalFees(fees)
//                .totalTransactions((long) transactions.size())
//                .build();
        return null;

    }

    /* =========================================================
       INTERNAL UTILITIES
       ========================================================= */

    private void changeStatus(Transaction tx,
                              TransactionStatus newStatus,
                              String reason) {

        TransactionStatus previous = tx.getStatus();

        tx.setStatus(newStatus);
        tx.setUpdatedAt(LocalDateTime.now());

       TransactionStateHistory transactionStateHistory = historyRepository.save(
                TransactionStateHistory.builder()
                        .transaction(tx)
                        .status(newStatus)              // ✅
                        .previousStatus(previous)
                        .changedAt(LocalDateTime.now())
                        .changedBy("SYSTEM")
                        .reason(reason)
                        .build()
        );

        log.info("TX {} : {} -> {}", tx.getReference(), previous, newStatus);
        log.info("TX changed at {} : from {} -> {}", transactionStateHistory.getChangedAt(), previous, newStatus);
    }

    private void blockFunds(Transaction tx) {
        walletServiceClient.blockAmount(
                tx.getSenderWalletId(),
                BalanceOperationRequest.builder()
                        .amount(effectiveDebitAmount(tx))
                        .transactionReference(tx.getReference())
                        .description("Blocage fonds")
                        .build()
        );
    }

    private void unblockFunds(Transaction tx) {
        walletServiceClient.unblockAmount(
                tx.getSenderWalletId(),
                BalanceOperationRequest.builder()
                        .amount(effectiveDebitAmount(tx))
                        .transactionReference(tx.getReference())
                        .description("Déblocage fonds")
                        .build()
        );
    }

    private void executeWalletOperations(Transaction tx) {

        BalanceOperationRequest request = BalanceOperationRequest.builder()
                .amount(tx.getAmount())
                .transactionReference(tx.getReference())
                .description(tx.getDescription())
                .build();

        switch (tx.getType()) {
            case DEBIT, WITHDRAWAL ->{
                    walletServiceClient.releaseBlockedAmount(
                            tx.getSenderWalletId(), request);
                    walletServiceClient.creditWallet(
                            tx.getReceiverWalletId(), request
                    );
            }

            case CREDIT, DEPOSIT ->
                    walletServiceClient.creditWallet(
                            tx.getReceiverWalletId(), request);

            case TRANSFER -> {
                walletServiceClient.releaseBlockedAmount(
                        tx.getSenderWalletId(), request);
                walletServiceClient.creditWallet(
                        tx.getReceiverWalletId(), request);
            }
        }
    }

    private BigDecimal effectiveDebitAmount(Transaction tx) {
        return tx.getAmount().add(tx.getFeeAmount() == null ? BigDecimal.ZERO : tx.getFeeAmount());
    }

    private boolean requiresDebit(Transaction tx) {
        return switch (tx.getType()) {
            case DEBIT, TRANSFER, WITHDRAWAL -> true;
            default -> false;
        };
    }

    private boolean canCancel(Transaction tx) {
        return tx.getStatus() == TransactionStatus.PENDING
                || tx.getStatus() == TransactionStatus.PROCESSING;
    }

    private BigDecimal sum(List<Transaction> txs, TransactionType... types) {
        return txs.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .filter(t -> List.of(types).contains(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Transaction findByReference(String ref) {
        return transactionRepository.findByReference(ref)
                .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException(ref));
    }

    private Transaction findByReferenceForUpdate(String ref) {
        return transactionRepository.findByReferenceWithLock(ref)
                .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException(ref));
    }

    private void validateRequest(CreateTransactionRequest request) {
        if (request.getType() == null || request.getAmount() == null || request.getSenderWalletNumber() == null
                || request.getReceiverWalletNumber() == null) {
            throw new TransactionExceptions.InvalidTransactionRequestException("Invalid request");
        }
    }
}
