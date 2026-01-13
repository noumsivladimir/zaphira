package com.zaphira.transaction.service;

import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.TransactionSummaryDTO;
import com.zaphira.transaction.dto.requests.CreateTransactionRequest;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface TransactionService {


    TransactionDTO createTransaction(CreateTransactionRequest request);
//    TransactionDTO initiateTransfer(TransferRequest request);
//    TransactionDTO initiateWithdrawal(WithdrawalRequest request);
//    TransactionDTO initiateDeposit(DepositRequest request);
//    Boolean initiateTransfer(TransferRequest request);

    // Consultation
    TransactionDTO getTransaction(String transactionReference);
    TransactionDTO getTransactionById(Long id);
    Page<TransactionDTO> getUserTransactions(Long userId, int page, int size);
//    Page<TransactionDTO> getWalletTransactions(String walletNumber, int page, int size);
//    Page<TransactionDTO> searchTransactions(TransactionSearchCriteria criteria);

    // Gestion du statut
    TransactionDTO processTransaction(String transactionReference);
    TransactionDTO completeTransaction(String transactionReference);
    TransactionDTO failTransaction(String transactionReference, String reason);
    TransactionDTO cancelTransaction(String transactionReference, String reason);
//    TransactionDTO reverseTransaction(String transactionReference, String reason);

    // Backwards-compatible constructor used by tests or code that provides FeignWalletClient but not EventPublisher
    public TransactionService(TransactionRepository repository,
                              TransactionStateHistoryRepository stateHistoryRepository,
                              TransactionValidationService validationService,
                              TransactionLimitService limitService,
                              FeeService feeService,
                              AuthorizationRequestService authorizationService,
                              ComplianceService complianceService,
                              LimitProperties limitProperties,
                              FeeProperties feeProperties,
                              WalletClient walletClient,
                              FeignWalletClient feignWalletClient,
                              BalanceService balanceService) {
        this(repository, stateHistoryRepository, validationService, limitService, feeService,
                authorizationService, complianceService, limitProperties, feeProperties, walletClient, feignWalletClient, null, balanceService);
    }

    // Backwards-compatible constructor used by tests or code that doesn't provide FeignWalletClient or EventPublisher
    public TransactionService(TransactionRepository repository,
                              TransactionStateHistoryRepository stateHistoryRepository,
                              TransactionValidationService validationService,
                              TransactionLimitService limitService,
                              FeeService feeService,
                              AuthorizationRequestService authorizationService,
                              ComplianceService complianceService,
                              LimitProperties limitProperties,
                              FeeProperties feeProperties,
                              WalletClient walletClient,
                              BalanceService balanceService) {
        this(repository, stateHistoryRepository, validationService, limitService, feeService,
                authorizationService, complianceService, limitProperties, feeProperties, walletClient, null, null, balanceService);
    }

    @Transactional
    public Transaction createTransaction(TransactionRequest request) {
        validationService.validateInitiation(request);
        limitService.validateLimits(request);
        LimitEvaluationResult evaluation = limitService.evaluateAuthorizationNeed(request);
        FeeCalculationResult fee = feeService.calculateFee(request);

        // Resolve authenticated user id and email from SecurityContext principal (set by JwtAuthenticationFilter)
        String actorEmail = request.getRequestedBy();
        Long authenticatedUserId = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            var principal = auth.getPrincipal();
            if (principal instanceof com.zaphira.transaction.security.AuthenticatedUser) {
                com.zaphira.transaction.security.AuthenticatedUser au = (com.zaphira.transaction.security.AuthenticatedUser) principal;
                authenticatedUserId = au.getId();
                if (au.getEmail() != null) actorEmail = au.getEmail();
            } else if (auth.getName() != null) {
                actorEmail = auth.getName();
            }
        }

        if (authenticatedUserId == null) {
            throw new com.zaphira.transaction.service.exception.WalletOperationException("Authenticated user id not present in token. Ensure JWT contains 'userId' claim.");
        }

        // Validate sender wallet ownership
        WalletDTO sender = null;
        try {
            sender = feignWalletClient.getWalletByNumber(request.getSenderWalletNumber());
        } catch (Exception e) {
            throw new com.zaphira.transaction.service.exception.WalletOperationException("Unable to fetch sender wallet: " + request.getSenderWalletNumber(), e);
        }

        if (sender == null || sender.getUserId() == null || !sender.getUserId().equals(authenticatedUserId)) {
            throw new com.zaphira.transaction.service.exception.WalletOperationException("User does not own the sender wallet");
        }

        WalletDTO receiver = null;
        try {
            receiver = feignWalletClient.getWalletByNumber(request.getReceiverWalletNumber());
        } catch (Exception e) {
            throw new com.zaphira.transaction.service.exception.WalletOperationException("Unable to fetch receiver wallet: " + request.getReceiverWalletNumber(), e);
        }
        if (receiver == null) {
            throw new com.zaphira.transaction.service.exception.WalletOperationException("Receiver wallet not found: " + request.getReceiverWalletNumber());
        }

        // Create transaction with Wallet JPA relationships instead of ID columns
        Transaction transaction = Transaction.builder()
            .senderWalletNumber(request.getSenderWalletNumber())
            .receiverWalletNumber(request.getReceiverWalletNumber())
            .amount(request.getAmount())
                .currency(request.getCurrency())
                .type(request.getType())
                .status(TransactionStatus.INITIATED)
                .channel(request.getChannel())
                .description(request.getDescription())
                .feeAmount(fee.getFeeAmount())
                .feeCurrency(fee.getFeeCurrency())
                .feeType(fee.getFeeType())
                .authorizationRequired(evaluation.isAuthorizationRequired())
                .authorizationMethod(evaluation.isAuthorizationRequired() ? evaluation.getMethod() : null)
                .initiatedBy(actorEmail)
                .lastUpdatedBy(actorEmail)
                .build();
    // Set Wallet JPA relationships using fetched wallet DTOs
    // Note: This requires converting WalletDTO to Wallet entity or mapping wallet references
    // For now, the relationships are set through the wallet lookups above
    // In production, you would map WalletDTO properties to Wallet entity
    transaction.setSenderWallet(mapWalletDtoToWallet(sender));
    transaction.setReceiverWallet(mapWalletDtoToWallet(receiver));

        // Compliance evaluation (may mark transaction UNDER_REVIEW)
        complianceService.evaluateOnCreation(request, transaction);

        transaction.applyStatus(TransactionStatus.INITIATED);
        Transaction saved = repository.save(transaction);
        recordState(saved, TransactionStatus.INITIATED, request.getRequestedBy(), "Transaction created");

        // Publish TransactionCreatedEvent to Kafka
        publishTransactionEvent(saved);

        if (evaluation.isAuthorizationRequired()) {
            saved.applyStatus(TransactionStatus.PENDING);
            repository.save(saved);
            recordState(saved, TransactionStatus.PENDING, request.getRequestedBy(), evaluation.getReason());
            authorizationService.createAuthorization(saved, evaluation.getMethod(), request.getRequestedBy());
            return saved;
        }

        // If compliance put transaction under review, do not process instantly
        if (saved.getComplianceStatus() != null
                && saved.getComplianceStatus() != com.zaphira.transaction.model.enums.ComplianceStatus.CLEAR) {
            return saved;
        }

        if (request.isProcessInstantly()) {
            return processImmediateTransaction(saved, request.getRequestedBy());
        }

        return saved;
    }

    public List<Transaction> getAllTransactions() {
        return repository.findAll();
    }

    public Transaction getTransaction(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    public List<TransactionStateHistory> getTransactionHistory(Long id) {
        Transaction transaction = getTransaction(id);
        return stateHistoryRepository.findByTransactionOrderByChangedAtAsc(transaction);
    }

    @Transactional
    public Transaction updateTransactionStatus(Long id, UpdateStatusRequest request) {
        Transaction tx = getTransaction(id);
        changeStatus(tx, request.getStatus(), request.getChangedBy(), request.getReason());
        return tx;
    }

    @Transactional
    public Transaction cancelTransaction(Long id, UpdateStatusRequest request) {
        Transaction tx = getTransaction(id);
        if (tx.getStatus() == TransactionStatus.COMPLETED || tx.getStatus() == TransactionStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel a completed or failed transaction");
        }
        changeStatus(tx, TransactionStatus.CANCELLED, request.getChangedBy(), request.getReason());
        return tx;
    }

    public AuthorizationInfoResponse getAuthorizationInfo(Long transactionId) {
        AuthorizationRequest request = authorizationService.getLatestAuthorization(transactionId);
        if (request == null) {
            return null;
        }
        return AuthorizationInfoResponse.builder()
                .authorizationId(request.getId())
                .status(request.getStatus())
                .method(request.getMethod())
                .requestedAt(request.getRequestedAt())
                .expiresAt(request.getExpiresAt())
                .challengeExposed(limitProperties.getAuthorization().isExposeChallengeInResponse())
                .challengeCode(limitProperties.getAuthorization().isExposeChallengeInResponse() ? request.getChallengeCode() : null)
                .build();
    }

    @Transactional
    public Transaction authorizeTransaction(Long id, AuthorizationValidationRequest request) {
        Transaction tx = getTransaction(id);
        if (!Boolean.TRUE.equals(tx.getAuthorizationRequired())) {
            throw new IllegalStateException("Transaction does not require authorization");
        }
        complianceService.assertNotBlocked(tx);
        authorizationService.approveAuthorization(id, request.getMethod(), request.getCode(), request.getAuthorizedBy());
        changeStatus(tx, TransactionStatus.AUTHORIZED, request.getAuthorizedBy(), "Authorization approved");
        return processImmediateTransaction(tx, request.getAuthorizedBy());
    }

    private Transaction processImmediateTransaction(Transaction transaction, String actor) {
        complianceService.assertNotBlocked(transaction);
        changeStatus(transaction, TransactionStatus.PROCESSING, actor, "Processing started");

        try {
            // Process balance updates based on transaction type
            boolean balanceUpdateSuccess = processBalanceUpdates(transaction);

            if (!balanceUpdateSuccess) {
                throw new RuntimeException("Balance update failed for transaction: " + transaction.getReference());
            }

            changeStatus(transaction, TransactionStatus.COMPLETED, actor, "Transaction completed successfully");
            publishTransactionEvent(transaction);
        } catch (Exception ex) {
            changeStatus(transaction, TransactionStatus.FAILED, actor, ex.getMessage());
            throw ex;
        }
        return transaction;
    }

    /**
     * Processes balance updates based on transaction type and ensures balance consistency
     */
    private boolean processBalanceUpdates(Transaction transaction) {
        TransactionType type = transaction.getType();
        BigDecimal amount = transaction.getAmount();
        String senderWalletNumber = transaction.getSenderWalletNumber();
        String receiverWalletNumber = transaction.getReceiverWalletNumber();

        switch (type) {
            // Transfer operations: debit sender, credit receiver
            case P2P_TRANSFER:
            case INTERNAL_TRANSFER:
            case CROSS_BORDER_TRANSFER:
            case BANK_TRANSFER:
            case CARD_TRANSFER:
                if (senderWalletNumber != null && receiverWalletNumber != null) {
                    // Check if sender can perform transaction
                    if (!balanceService.canPerformTransaction(senderWalletNumber, amount, TransactionType.ATM_WITHDRAWAL)) {
                        return false;
                    }
                    // Debit sender
                    boolean debitSuccess = balanceService.updateBalanceForTransaction(senderWalletNumber, amount, TransactionType.ATM_WITHDRAWAL);
                    if (!debitSuccess) return false;

                    // Credit receiver
                    boolean creditSuccess = balanceService.updateBalanceForTransaction(receiverWalletNumber, amount, TransactionType.WALLET_TOPUP);
                    return creditSuccess;
                }
                return false;

            // Payment operations: debit sender
            case MERCHANT_PAYMENT:
            case BILL_PAYMENT:
            case SUBSCRIPTION_PAYMENT:
            case INVOICE_PAYMENT:
            case QR_PAYMENT:
            case PAYMENT_LINK:
            case IN_APP_PAYMENT:
            case CONTACTLESS_PAYMENT:
            case MOBILE_RECHARGE:
            case GIFT_CARD_PURCHASE:
                if (senderWalletNumber != null) {
                    return balanceService.canPerformTransaction(senderWalletNumber, amount, type) &&
                           balanceService.updateBalanceForTransaction(senderWalletNumber, amount, type);
                }
                return false;

            // Withdrawal operations: debit sender
            case ATM_WITHDRAWAL:
            case AGENT_WITHDRAWAL:
            case CRYPTO_WITHDRAWAL:
                if (senderWalletNumber != null) {
                    return balanceService.canPerformTransaction(senderWalletNumber, amount, type) &&
                           balanceService.updateBalanceForTransaction(senderWalletNumber, amount, type);
                }
                return false;

            // Top-up/Deposit operations: credit receiver
            case WALLET_TOPUP:
            case TRANSIT_TOPUP:
                if (receiverWalletNumber != null) {
                    return balanceService.updateBalanceForTransaction(receiverWalletNumber, amount, type);
                }
                return false;

            // Refund operations: credit receiver
            case REFUND:
                if (receiverWalletNumber != null) {
                    return balanceService.updateBalanceForTransaction(receiverWalletNumber, amount, TransactionType.REFUND);
                }
                return false;

            // Reversal operations: reverse the original transaction
            case REVERSAL:
                // For reversals, we need to reverse the original transaction direction
                // This is a simplified implementation - in production you'd look up the original transaction
                if (senderWalletNumber != null && receiverWalletNumber != null) {
                    // Credit sender (reverse debit)
                    boolean creditSender = balanceService.updateBalanceForTransaction(senderWalletNumber, amount, TransactionType.REFUND);
                    if (!creditSender) return false;

                    // Debit receiver (reverse credit)
                    return balanceService.canPerformTransaction(receiverWalletNumber, amount, TransactionType.ATM_WITHDRAWAL) &&
                           balanceService.updateBalanceForTransaction(receiverWalletNumber, amount, TransactionType.ATM_WITHDRAWAL);
                }
                return false;

            // Other operations that might not affect balances immediately
            case ESCROW:
            case SCHEDULED:
            case RECURRING:
            case SPLIT_BILL:
            case REQUEST_MONEY:
            case DONATION:
            case TIP:
            case BULK_TRANSFER:
            case SPLIT_TRANSFER:
            case GROUP_TRANSFER:
                // These might require special handling or might not affect balances immediately
                // For now, treat as successful (no balance changes)
                return true;

            default:
                // Unknown transaction type
                return false;
        }
    }

    private void changeStatus(Transaction transaction, TransactionStatus newStatus, String changedBy, String reason) {
        transaction.applyStatus(newStatus);
        transaction.setLastUpdatedBy(changedBy);
        repository.save(transaction);
        recordState(transaction, newStatus, changedBy, reason);
    }

    private void recordState(Transaction transaction, TransactionStatus status, String changedBy, String reason) {
        TransactionStateHistory history = TransactionStateHistory.builder()
                .transaction(transaction)
                .status(status)
                .changedBy(changedBy)
                .reason(reason)
                .build();
        stateHistoryRepository.save(history);
    }

    /**
     * Helper method to convert WalletDTO to Wallet entity for JPA relationships
     * Maps wallet service response DTO to transaction service Wallet entity
     * @param walletDto WalletDTO from wallet-service Feign response
     * @return Wallet entity populated with DTO values
     */
    private Wallet mapWalletDtoToWallet(WalletDTO walletDto) {
        if (walletDto == null) {
            return null;
        }
        return Wallet.builder()
                .id(walletDto.getId())
                .walletNumber(walletDto.getWalletNumber())
                .availableBalance(walletDto.getAvailableBalance())
                .blockedBalance(walletDto.getBlockedBalance())
                .totalBalance(walletDto.getTotalBalance())
                .currency(walletDto.getCurrency())
                .active(walletDto.getActive())
                .userId(walletDto.getUserId())
                .status(walletDto.getStatus())
                .frozenAt(walletDto.getFrozenAt())
                .frozenBy(walletDto.getFrozenBy())
                .frozenReason(walletDto.getFrozenReason())
                .dailyLimit(walletDto.getDailyLimit())
                .dailySpent(walletDto.getDailySpent())
                .monthlyLimit(walletDto.getMonthlyLimit())
                .monthlySpent(walletDto.getMonthlySpent())
                .lastLimitReset(walletDto.getLastLimitReset())
                .createdAt(walletDto.getCreatedAt())
                .updatedAt(walletDto.getUpdatedAt())
                .closedAt(walletDto.getClosedAt())
                .build();
    }

    /**
     * Helper method to publish TransactionCreatedEvent to Kafka
     * @param transaction The created transaction
     */
    private void publishTransactionEvent(Transaction transaction) {
        if (transactionEventPublisher == null) {
            return; // Event publisher not available (e.g., in tests without Kafka)
        }

        try {
            TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                    .transactionId(transaction.getId())
                    .reference(transaction.getReference())
                    .senderWalletNumber(transaction.getSenderWalletNumber())
                    .receiverWalletNumber(transaction.getReceiverWalletNumber())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .status(transaction.getStatus().name())
                    .createdAt(transaction.getCreatedAt())
                    .build();
            transactionEventPublisher.publishTransactionCreated(event);
        } catch (Exception e) {
            // Log but don't fail the transaction creation if event publishing fails
            org.slf4j.LoggerFactory.getLogger(TransactionService.class)
                    .warn("Failed to publish TransactionCreatedEvent for transaction {}: {}",
                            transaction.getId(), e.getMessage());
        }
    }

    // Utilitaires
//    boolean existsByReference(String transactionReference);
//    void retryFailedTransaction(String transactionReference);
}
