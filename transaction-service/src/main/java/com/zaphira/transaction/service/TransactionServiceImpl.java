package com.zaphira.transaction.service;

import com.zaphira.common.dto.WalletSummaryDTO;
import com.zaphira.common.dto.request.BalanceOperationRequest;
import com.zaphira.common.model.enums.Currency;
import com.zaphira.transaction.client.WalletServiceClient;
import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.TransactionSummaryDTO;
import com.zaphira.transaction.dto.requests.CreateTransactionRequest;
import com.zaphira.transaction.dto.requests.TransferRequest;
import com.zaphira.transaction.dto.requests.PaymentRequest;
import com.zaphira.transaction.dto.requests.BulkTransferRequest;
import com.zaphira.transaction.dto.requests.SplitPaymentRequest;
import com.zaphira.transaction.dto.response.SplitPaymentResponse;
import com.zaphira.transaction.event.TransactionEventPublisher;
import com.zaphira.transaction.exception.TransactionExceptions;
import com.zaphira.transaction.mapper.TransactionMapper;
import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.model.entities.TransactionStateHistory;
import com.zaphira.transaction.model.enums.TransactionCategory;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.repository.TransactionRepository;
import com.zaphira.transaction.repository.TransactionStateHistoryRepository;
import com.zaphira.transaction.security.AuthenticatedUser;
import com.zaphira.transaction.util.TransactionReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    private static final int MAX_BULK_ITEMS = 100;
    private static final BigDecimal MAX_BULK_TOTAL = new BigDecimal("1000000");

    /* =========================================================
       CREATE
       ========================================================= */

    @Override
    public TransactionDTO createTransaction(CreateTransactionRequest request) {

//        WalletIdResponse senderWalletIdResponse = walletServiceClient.getWalletIdByWalletNumber(request.getSenderWalletNumber());
//        WalletIdResponse  receiverWalletIdResponse = walletServiceClient.getWalletIdByWalletNumber(request.getReceiverWalletNumber());


        WalletSummaryDTO senderWalletIdSummary = walletServiceClient.getWalletSummaryByWalletNumber(request.getSenderWalletNumber());
        WalletSummaryDTO receiverWalletSummary = walletServiceClient.getWalletSummaryByWalletNumber(request.getReceiverWalletNumber());

        AuthenticatedUser currentUser = requireUser();
        enforceWalletOwnership(currentUser, senderWalletIdSummary);

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

            transactionRepository.save(transaction);

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

            transactionRepository.save(transaction);

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

            transactionRepository.save(transaction);

            log.info("Created transaction with id {}", transaction.getId());
            changeStatus(transaction, TransactionStatus.PENDING, "Transaction créée");

            eventPublisher.publishTransactionCreated(transaction);

            return processTransaction(transaction.getReference());

        } else if (request.getType() == TransactionType.DEBIT) {
            Transaction transaction = Transaction.builder()

                    .reference(referenceGenerator.generate())
                    .senderWalletId(senderWalletIds)
                    .senderWalletNumber(senderWalletIdSummary.getWalletNumber())
                    .receiverWalletNumber(receiverWalletSummary.getWalletNumber())
                    .receiverWalletId(receiverWalletIds)
                    .type(TransactionType.DEBIT)
                    .category(request.getCategory())
                    .channel(request.getChannel())
                    .status(TransactionStatus.PENDING)
                    .amount(request.getAmount())
                    .feeAmount(BigDecimal.ZERO)
                    .currency(Currency.XAF)
                    .description(request.getDescription())
                    .build();

            transactionRepository.save(transaction);

            log.info("Created transaction with id {}", transaction.getId());
            changeStatus(transaction, TransactionStatus.PENDING, "Transaction créée");

            eventPublisher.publishTransactionCreated(transaction);

            return processTransaction(transaction.getReference());

        }

        throw new TransactionExceptions.InvalidTransactionRequestException("Unsupported transaction type");


    }

    @Override
    public TransactionDTO createTransfer(TransferRequest request) {
        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
                .type(TransactionType.TRANSFER)
                .category(request.getCategory())
                .channel(request.getChannel())
                .amount(request.getAmount())
                .senderWalletNumber(request.getSenderWalletNumber())
                .receiverWalletNumber(request.getReceiverWalletNumber())
                .description(request.getDescription())
                .build();
        return createTransaction(txRequest);
    }

    @Override
    public TransactionDTO createDeposit(TransferRequest request) {
        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
                .type(TransactionType.DEPOSIT)
                .category(request.getCategory())
                .channel(request.getChannel())
                .amount(request.getAmount())
                .senderWalletNumber(request.getSenderWalletNumber())
                .receiverWalletNumber(request.getReceiverWalletNumber())
                .description(request.getDescription())
                .build();
        return createTransaction(txRequest);
    }

    @Override
    public TransactionDTO createWithdrawal(TransferRequest request) {
        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
                .type(TransactionType.WITHDRAWAL)
                .category(request.getCategory())
                .channel(request.getChannel())
                .amount(request.getAmount())
                .senderWalletNumber(request.getSenderWalletNumber())
                .receiverWalletNumber(request.getReceiverWalletNumber())
                .description(request.getDescription())
                .build();
        return createTransaction(txRequest);
    }

    @Override
    public TransactionDTO createMerchantPayment(PaymentRequest request) {
        WalletSummaryDTO merchantWallet = walletServiceClient.getWalletSummaryByWalletNumber(request.getMerchantWalletNumber());
        if (merchantWallet == null || merchantWallet.getMerchant() == null || !merchantWallet.getMerchant()) {
            throw new TransactionExceptions.InvalidTransactionRequestException("Receiver wallet is not a merchant wallet");
        }

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
                .type(TransactionType.PAYMENT)
                .category(request.getCategory())
                .channel(request.getChannel())
                .amount(request.getAmount())
                .senderWalletNumber(request.getPayerWalletNumber())
                .receiverWalletNumber(request.getMerchantWalletNumber())
                .description(request.getDescription())
                .build();
        return createTransaction(txRequest);
    }

    @Override
    public Page<TransactionDTO> createBulkTransfer(BulkTransferRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new TransactionExceptions.InvalidTransactionRequestException("Bulk transfer requires items");
        }

        if (request.getItems().size() > MAX_BULK_ITEMS) {
            throw new TransactionExceptions.InvalidTransactionRequestException("Bulk transfer exceeds max items: " + MAX_BULK_ITEMS);
        }

        // Enforce single sender across all items
        String senderWalletNumber = request.getSenderWalletNumber();
        if (senderWalletNumber == null || senderWalletNumber.isBlank()) {
            throw new TransactionExceptions.InvalidTransactionRequestException("Sender wallet number is required");
        }

        BigDecimal total = BigDecimal.ZERO;

        List<CreateTransactionRequest> txRequests = request.getItems().stream()
                .map(item -> CreateTransactionRequest.builder()
                        .type(TransactionType.TRANSFER)
                        .category(item.getCategory())
                        .channel(item.getChannel())
                        .amount(item.getAmount())
                        .senderWalletNumber(senderWalletNumber)
                        .receiverWalletNumber(item.getReceiverWalletNumber())
                        .description(item.getDescription() != null ? item.getDescription() : request.getDescription())
                        .build())
                .collect(Collectors.toList());

        for (CreateTransactionRequest tx : txRequests) {
            if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new TransactionExceptions.InvalidTransactionRequestException("Bulk item amount must be positive");
            }
            total = total.add(tx.getAmount());
        }

        if (total.compareTo(MAX_BULK_TOTAL) > 0) {
            throw new TransactionExceptions.InvalidTransactionRequestException("Bulk transfer total exceeds limit");
        }

        // All-or-nothing: rely on transactional boundary of the service bean
        List<TransactionDTO> results = txRequests.stream()
                .map(this::createTransaction)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(results);
    }

    /**
     * LOT 2: Split Payment - Divide a payment among multiple recipients.
     * 
     * Business logic:
     * 1. Validate: sum of recipient amounts must equal totalAmount
     * 2. Validate: at least 2 recipients
     * 3. Create individual transactions for each recipient
     * 4. All-or-nothing: if any fails, rollback all (@Transactional)
     * 5. Return summary with status (SUCCESS/PARTIAL/FAILED)
     */
    @Override
    @Transactional
    public SplitPaymentResponse createSplitPayment(SplitPaymentRequest request) {
        // Validation: at least 2 recipients
        if (request.getRecipients() == null || request.getRecipients().size() < 2) {
            throw new TransactionExceptions.InvalidTransactionRequestException(
                "Split payment requires at least 2 recipients");
        }

        // Validation: sum of recipient amounts must equal total
        BigDecimal sumOfShares = request.getRecipients().stream()
            .map(SplitPaymentRequest.RecipientShare::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumOfShares.compareTo(request.getTotalAmount()) != 0) {
            throw new TransactionExceptions.InvalidTransactionRequestException(
                String.format("Sum of recipient shares (%s) must equal total amount (%s)", 
                    sumOfShares, request.getTotalAmount()));
        }

        // Generate batch ID for tracking
        String batchId = "SPLIT-" + System.currentTimeMillis();
        List<TransactionDTO> transactions = new ArrayList<>();
        int successCount = 0;

        try {
            // Create a transaction for each recipient
            for (SplitPaymentRequest.RecipientShare recipient : request.getRecipients()) {
                try {
                    CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
                        .type(TransactionType.TRANSFER)
                        .category(TransactionCategory.WALLET_TO_WALLET)
                        .channel(com.zaphira.transaction.model.enums.TransactionChannel.BATCH)
                        .amount(recipient.getAmount())
                        .senderWalletNumber(request.getSenderWalletNumber())
                        .receiverWalletNumber(recipient.getRecipientWalletNumber())
                        .description(String.format("Split payment - %s | Batch: %s | %s", 
                            recipient.getLabel() != null ? recipient.getLabel() : "Your share",
                            batchId,
                            request.getDescription() != null ? request.getDescription() : ""))
                        .build();

                    TransactionDTO transaction = createTransaction(txRequest);
                    transactions.add(transaction);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to create split payment transaction for recipient {}: {}", 
                        recipient.getRecipientWalletNumber(), e.getMessage());
                    // In @Transactional, exception will rollback all
                    throw e;
                }
            }

            // All succeeded
            return SplitPaymentResponse.builder()
                .batchId(batchId)
                .status(SplitPaymentResponse.SplitStatus.SUCCESS)
                .totalAmount(request.getTotalAmount())
                .totalRecipients(request.getRecipients().size())
                .successfulCount(successCount)
                .failedCount(0)
                .transactions(transactions)
                .build();

        } catch (Exception e) {
            // Rollback occurred, all failed
            return SplitPaymentResponse.builder()
                .batchId(batchId)
                .status(SplitPaymentResponse.SplitStatus.FAILED)
                .totalAmount(request.getTotalAmount())
                .totalRecipients(request.getRecipients().size())
                .successfulCount(0)
                .failedCount(request.getRecipients().size())
                .transactions(transactions)
                .errorMessage("Split payment failed: " + e.getMessage())
                .build();
        }
    }

    /* =========================================================
       PROCESS
       ========================================================= */

    @Override
    public TransactionDTO processTransaction(String reference) {

        Transaction tx = findByReferenceForUpdate(reference);
        ensureCanAccessTransaction(tx);

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

        transactionRepository.save(tx);
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
        ensureCanAccessTransaction(tx);

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
        Transaction tx = findByReference(reference);
        ensureCanAccessTransaction(tx);
        return transactionMapper.toDTO(tx);
    }

    @Override
    public TransactionDTO getTransactionById(Long id) {
        Transaction tx = transactionRepository.findById(id)
            .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException("TX not found"));
        ensureCanAccessTransaction(tx);
        return transactionMapper.toDTO(tx);
    }

    @Override
    public Page<TransactionDTO> getUserTransactions(Long userId, int page, int size) {
        throw new TransactionExceptions.InvalidTransactionRequestException("Listing by userId is not supported; use wallet listing");
    }

    @Override
    public Page<TransactionDTO> getWalletTransactions(String walletNumber, int page, int size) {
        WalletSummaryDTO wallet = walletServiceClient.getWalletSummaryByWalletNumber(walletNumber);
        if (wallet == null || wallet.getWalletId() == null) {
            throw new TransactionExceptions.InvalidTransactionRequestException("Wallet not found: " + walletNumber);
        }

        AuthenticatedUser currentUser = requireUser();
        enforceWalletOwnership(currentUser, wallet);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.findByWalletId(wallet.getWalletId(), pageable)
                .map(transactionMapper::toDTO);
    }

    @Override
    public Page<TransactionDTO> searchTransactions(TransactionStatus status,
                                                   TransactionType type,
                                                   String currency,
                                                   BigDecimal minAmount,
                                                   BigDecimal maxAmount,
                                                   LocalDate from,
                                                   LocalDate to,
                                                   String reference,
                                                   int page,
                                                   int size) {

        AuthenticatedUser currentUser = requireUser();
        if (!hasRole(currentUser, "ADMIN")) {
            throw new TransactionExceptions.InvalidTransactionRequestException("Search is restricted to administrators");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        LocalDateTime fromDate = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDate = to != null ? to.atTime(23, 59, 59) : null;

        return transactionRepository.search(
                        status,
                        type,
                        currency,
                        minAmount,
                        maxAmount,
                        fromDate,
                        toDate,
                        reference,
                        pageable
                )
                .map(transactionMapper::toDTO);
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
       REVERSE / REFUND / RETRY
       ========================================================= */

    @Override
    public TransactionDTO reverseTransaction(String reference, String reason) {
        Transaction original = findByReferenceForUpdate(reference);
        ensureCanAccessTransaction(original);

        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new TransactionExceptions.InvalidTransactionStatusException("Only completed transactions can be reversed");
        }

        Transaction reversal = Transaction.builder()
                .reference(referenceGenerator.generate())
                .senderWalletId(original.getReceiverWalletId())
                .senderWalletNumber(original.getReceiverWalletNumber())
                .receiverWalletId(original.getSenderWalletId())
                .receiverWalletNumber(original.getSenderWalletNumber())
                .type(TransactionType.REVERSAL)
                .category(original.getCategory())
                .channel(original.getChannel())
                .status(TransactionStatus.PENDING)
                .amount(original.getAmount())
                .feeAmount(BigDecimal.ZERO)
                .currency(original.getCurrency())
                .description("Reversal of " + original.getReference() + (reason != null ? " - " + reason : ""))
                .relatedTransactionId(original.getId())
                .build();

        transactionRepository.save(reversal);
        changeStatus(reversal, TransactionStatus.PENDING, "Reversal initiated");

        BalanceOperationRequest request = BalanceOperationRequest.builder()
                .amount(original.getAmount())
                .transactionReference(reversal.getReference())
                .description(reversal.getDescription())
                .build();

        changeStatus(reversal, TransactionStatus.PROCESSING, "Moving funds for reversal");
        try {
            walletServiceClient.debitWallet(original.getReceiverWalletId(), request);
            walletServiceClient.creditWallet(original.getSenderWalletId(), request);

            changeStatus(reversal, TransactionStatus.COMPLETED, "Reversal completed");
            transactionRepository.save(reversal);
            eventPublisher.publishTransactionCompleted(reversal);

            changeStatus(original, TransactionStatus.REVERSED, reason != null ? reason : "Reversed");
            original.setFailureReason(reason);
            transactionRepository.save(original);

            return transactionMapper.toDTO(reversal);
        } catch (Exception ex) {
            log.error("Reversal failed for {}", reference, ex);
            changeStatus(reversal, TransactionStatus.FAILED, ex.getMessage());
            reversal.setFailureReason(ex.getMessage());
            transactionRepository.save(reversal);
            return transactionMapper.toDTO(reversal);
        }
    }

    @Override
    public TransactionDTO refundTransaction(String reference, String reason) {
        Transaction original = findByReferenceForUpdate(reference);
        ensureCanAccessTransaction(original);

        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new TransactionExceptions.InvalidTransactionStatusException("Only completed transactions can be refunded");
        }

        Transaction refund = Transaction.builder()
                .reference(referenceGenerator.generate())
                .senderWalletId(original.getReceiverWalletId())
                .senderWalletNumber(original.getReceiverWalletNumber())
                .receiverWalletId(original.getSenderWalletId())
                .receiverWalletNumber(original.getSenderWalletNumber())
                .type(TransactionType.REFUND)
                .category(original.getCategory())
                .channel(original.getChannel())
                .status(TransactionStatus.PENDING)
                .amount(original.getAmount())
                .feeAmount(BigDecimal.ZERO)
                .currency(original.getCurrency())
                .description("Refund of " + original.getReference() + (reason != null ? " - " + reason : ""))
                .relatedTransactionId(original.getId())
                .build();

        transactionRepository.save(refund);
        changeStatus(refund, TransactionStatus.PENDING, "Refund initiated");

        BalanceOperationRequest request = BalanceOperationRequest.builder()
                .amount(original.getAmount())
                .transactionReference(refund.getReference())
                .description(refund.getDescription())
                .build();

        changeStatus(refund, TransactionStatus.PROCESSING, "Moving funds for refund");
        try {
            walletServiceClient.debitWallet(original.getReceiverWalletId(), request);
            walletServiceClient.creditWallet(original.getSenderWalletId(), request);

            changeStatus(refund, TransactionStatus.COMPLETED, "Refund completed");
            transactionRepository.save(refund);
            eventPublisher.publishTransactionCompleted(refund);

            changeStatus(original, TransactionStatus.REVERSED, reason != null ? reason : "Refunded");
            original.setFailureReason(reason);
            transactionRepository.save(original);

            return transactionMapper.toDTO(refund);
        } catch (Exception ex) {
            log.error("Refund failed for {}", reference, ex);
            changeStatus(refund, TransactionStatus.FAILED, ex.getMessage());
            refund.setFailureReason(ex.getMessage());
            transactionRepository.save(refund);
            return transactionMapper.toDTO(refund);
        }
    }

    @Override
    public TransactionDTO retryFailedTransaction(String transactionReference) {
        Transaction tx = findByReferenceForUpdate(transactionReference);
        ensureCanAccessTransaction(tx);

        if (!tx.getStatus().canBeRetried()) {
            throw new TransactionExceptions.InvalidTransactionStatusException("Only failed transactions can be retried");
        }

        tx.setFailureReason(null);
        changeStatus(tx, TransactionStatus.PENDING, "Retry requested");
        transactionRepository.save(tx);

        return processTransaction(transactionReference);
    }

    /* =========================================================
       INTERNAL UTILITIES
       ========================================================= */

    private AuthenticatedUser requireUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser au) {
            return au;
        }
        throw new TransactionExceptions.InvalidTransactionRequestException("Unauthenticated request");
    }

    private boolean hasRole(AuthenticatedUser user, String role) {
        if (user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(r -> r.equalsIgnoreCase(role) || r.equalsIgnoreCase("ROLE_" + role));
    }

    private void enforceWalletOwnership(AuthenticatedUser user, WalletSummaryDTO wallet) {
        if (wallet == null || wallet.getUserId() == null) {
            throw new TransactionExceptions.InvalidTransactionRequestException("Wallet ownership cannot be verified");
        }

        if (hasRole(user, "ADMIN")) {
            return;
        }

        if (!wallet.getUserId().equals(user.getId())) {
            throw new TransactionExceptions.InvalidTransactionRequestException("Forbidden: wallet not owned by requester");
        }
    }

    private void ensureCanAccessTransaction(Transaction tx) {
        AuthenticatedUser user = requireUser();
        if (hasRole(user, "ADMIN")) {
            return;
        }

        WalletSummaryDTO sender = safeWalletSummary(tx.getSenderWalletNumber());
        WalletSummaryDTO receiver = safeWalletSummary(tx.getReceiverWalletNumber());

        if (isOwnerOfWallet(user, sender) || isOwnerOfWallet(user, receiver)) {
            return;
        }

        throw new TransactionExceptions.InvalidTransactionRequestException("Forbidden: transaction not accessible by requester");
    }

    private WalletSummaryDTO safeWalletSummary(String walletNumber) {
        if (walletNumber == null) {
            return null;
        }
        try {
            return walletServiceClient.getWalletSummaryByWalletNumber(walletNumber);
        } catch (Exception e) {
            log.warn("Unable to fetch wallet summary for {}", walletNumber, e);
            return null;
        }
    }

    private boolean isOwnerOfWallet(AuthenticatedUser user, WalletSummaryDTO summary) {
        return summary != null && summary.getUserId() != null && summary.getUserId().equals(user.getId());
    }

    private void changeStatus(Transaction tx,
                              TransactionStatus newStatus,
                              String reason) {

        TransactionStatus previous = tx.getStatus();

        tx.setStatus(newStatus);
        tx.setUpdatedAt(LocalDateTime.now());

       TransactionStateHistory transactionStateHistory = historyRepository.save(
                TransactionStateHistory.builder()
                        .transaction(tx)
                        .newStatus(newStatus)              // ✅
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
            
            case PAYMENT, MERCHANT_PAYMENT -> {
                walletServiceClient.releaseBlockedAmount(
                        tx.getSenderWalletId(), request);
                walletServiceClient.creditWallet(
                        tx.getReceiverWalletId(), request);
            }
            
            case REVERSAL, REFUND -> {
                // Already handled in reverseTransaction/refundTransaction methods
            }
            
            case FEE -> {
                // Fee collection - credit to system wallet
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

    // ========== LOT 4: User-scoped Search Methods ==========

    @Override
    public Page<TransactionDTO> getMyTransactions(TransactionStatus status, TransactionType type,
                                                   LocalDate from, LocalDate to, int page, int size) {
        Long currentUserId = com.zaphira.common.security.SecurityContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            throw new TransactionExceptions.InvalidTransactionRequestException("No authenticated user");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = (to != null) ? to.atTime(23, 59, 59) : null;

        Page<Transaction> transactions;

        if (status != null && type != null && fromDateTime != null && toDateTime != null) {
            transactions = transactionRepository.findByUserIdAndStatusAndTypeAndDateRange(
                    currentUserId, status, type, fromDateTime, toDateTime, pageable);
        } else if (status != null && type != null) {
            transactions = transactionRepository.findByUserIdAndStatusAndType(
                    currentUserId, status, type, pageable);
        } else if (status != null) {
            transactions = transactionRepository.findByUserIdAndStatus(
                    currentUserId, status, pageable);
        } else if (type != null) {
            transactions = transactionRepository.findByUserIdAndType(
                    currentUserId, type, pageable);
        } else {
            transactions = transactionRepository.findByUserId(currentUserId, pageable);
        }

        return transactions.map(transactionMapper::toDTO);
    }

    @Override
    public Page<TransactionDTO> getMySentTransactions(TransactionStatus status, int page, int size) {
        Long currentUserId = com.zaphira.common.security.SecurityContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            throw new TransactionExceptions.InvalidTransactionRequestException("No authenticated user");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Transaction> transactions = (status != null)
                ? transactionRepository.findBySenderWalletIdAndStatus(currentUserId, status, pageable)
                : transactionRepository.findBySenderWalletId(currentUserId, pageable);

        return transactions.map(transactionMapper::toDTO);
    }

    @Override
    public Page<TransactionDTO> getMyReceivedTransactions(TransactionStatus status, int page, int size) {
        Long currentUserId = com.zaphira.common.security.SecurityContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            throw new TransactionExceptions.InvalidTransactionRequestException("No authenticated user");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Transaction> transactions = (status != null)
                ? transactionRepository.findByReceiverWalletIdAndStatus(currentUserId, status, pageable)
                : transactionRepository.findByReceiverWalletId(currentUserId, pageable);

        return transactions.map(transactionMapper::toDTO);
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
