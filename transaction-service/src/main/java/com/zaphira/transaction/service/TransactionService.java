package com.zaphira.transaction.service;

import com.zaphira.transaction.config.FeeProperties;
import com.zaphira.transaction.config.LimitProperties;
import com.zaphira.transaction.dto.AuthorizationInfoResponse;
import com.zaphira.transaction.dto.AuthorizationValidationRequest;
import com.zaphira.transaction.dto.TransactionRequest;
import com.zaphira.transaction.dto.UpdateStatusRequest;
import com.zaphira.transaction.integration.wallet.WalletClient;
import com.zaphira.transaction.integration.wallet.WalletTransferRequest;
import com.zaphira.transaction.model.AuthorizationRequest;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.TransactionStateHistory;
import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.repository.TransactionRepository;
import com.zaphira.transaction.repository.TransactionStateHistoryRepository;
import com.zaphira.transaction.service.authorization.TransactionAuthorizationService;
import com.zaphira.transaction.service.compliance.ComplianceService;
import com.zaphira.transaction.service.fee.FeeCalculationResult;
import com.zaphira.transaction.service.fee.FeeService;
import com.zaphira.transaction.service.limit.LimitEvaluationResult;
import com.zaphira.transaction.service.limit.TransactionLimitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final TransactionStateHistoryRepository stateHistoryRepository;
    private final TransactionValidationService validationService;
    private final TransactionLimitService limitService;
    private final FeeService feeService;
    private final TransactionAuthorizationService authorizationService;
    private final ComplianceService complianceService;
    private final LimitProperties limitProperties;
    private final FeeProperties feeProperties;
    private final WalletClient walletClient;

    public TransactionService(TransactionRepository repository,
                              TransactionStateHistoryRepository stateHistoryRepository,
                              TransactionValidationService validationService,
                              TransactionLimitService limitService,
                              FeeService feeService,
                              TransactionAuthorizationService authorizationService,
                              ComplianceService complianceService,
                              LimitProperties limitProperties,
                              FeeProperties feeProperties,
                              WalletClient walletClient) {
        this.repository = repository;
        this.stateHistoryRepository = stateHistoryRepository;
        this.validationService = validationService;
        this.limitService = limitService;
        this.feeService = feeService;
        this.authorizationService = authorizationService;
        this.complianceService = complianceService;
        this.limitProperties = limitProperties;
        this.feeProperties = feeProperties;
        this.walletClient = walletClient;
    }

    @Transactional
    public Transaction createTransaction(TransactionRequest request) {
        validationService.validateInitiation(request);
        limitService.validateLimits(request);
        LimitEvaluationResult evaluation = limitService.evaluateAuthorizationNeed(request);
        FeeCalculationResult fee = feeService.calculateFee(request);

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
                .authorizationMethod(evaluation.isAuthorizationRequired() ? evaluation.getMethod() : AuthorizationMethod.NONE)
                .initiatedBy(request.getRequestedBy())
                .lastUpdatedBy(request.getRequestedBy())
                .build();

        // Compliance evaluation (may mark transaction UNDER_REVIEW)
        complianceService.evaluateOnCreation(request, transaction);

        transaction.applyStatus(TransactionStatus.INITIATED);
        Transaction saved = repository.save(transaction);
        recordState(saved, TransactionStatus.INITIATED, request.getRequestedBy(), "Transaction created");

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
            walletClient.executeTransfer(WalletTransferRequest.builder()
                    .reference(transaction.getReference())
                    .senderWalletNumber(transaction.getSenderWalletNumber())
                    .receiverWalletNumber(transaction.getReceiverWalletNumber())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .description(transaction.getDescription())
                    .build());
            changeStatus(transaction, TransactionStatus.COMPLETED, actor, "Transaction completed successfully");
        } catch (Exception ex) {
            changeStatus(transaction, TransactionStatus.FAILED, actor, ex.getMessage());
            throw ex;
        }
        return transaction;
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
}
