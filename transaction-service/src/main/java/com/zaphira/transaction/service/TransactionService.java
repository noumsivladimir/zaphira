package com.zaphira.transaction.service;

import com.zaphira.common.model.entities.Wallet;
import com.zaphira.transaction.config.FeeProperties;
import com.zaphira.transaction.config.LimitProperties;
import com.zaphira.transaction.dto.AuthorizationInfoResponse;
import com.zaphira.transaction.dto.AuthorizationValidationRequest;
import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.TransactionRequest;
import com.zaphira.transaction.dto.TransactionStateHistoryDTO;
import com.zaphira.transaction.dto.UpdateStatusRequest;
import com.zaphira.transaction.dto.mapper.TransactionDTOConverterUtil;
import com.zaphira.transaction.dto.mapper.TransactionDTOMapper;
import com.zaphira.transaction.integration.wallet.FeignWalletClient;
import com.zaphira.transaction.integration.wallet.WalletClient;
import com.zaphira.transaction.integration.wallet.WalletTransferRequest;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.TransactionStateHistory;
import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.model.AuthorizationRequest;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.repository.TransactionRepository;
import com.zaphira.transaction.repository.TransactionStateHistoryRepository;
import com.zaphira.transaction.service.authorization.TransactionAuthorizationService;
import com.zaphira.transaction.service.compliance.ComplianceService;
import com.zaphira.transaction.service.fee.FeeCalculationResult;
import com.zaphira.transaction.service.fee.FeeService;
import com.zaphira.transaction.service.limit.LimitEvaluationResult;
import com.zaphira.transaction.service.limit.TransactionLimitService;
import com.zaphira.transaction.service.otp.OtpService;
import com.zaphira.common.dto.WalletDTO;
import com.zaphira.common.event.TransactionCreatedEvent;
import com.zaphira.common.event.TransactionCompletedEvent;
import com.zaphira.transaction.event.TransactionEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private TransactionRepository repository;
    private TransactionStateHistoryRepository stateHistoryRepository;
    private TransactionValidationService validationService;
    private TransactionLimitService limitService;
    private FeeService feeService;
    private TransactionAuthorizationService authorizationService;
    private ComplianceService complianceService;
    private WalletClient walletClient;
    private FeignWalletClient feignWalletClient;
    private TransactionEventPublisher transactionEventPublisher;
    private TransactionDTOMapper dtoMapper;
    private TransactionDTOConverterUtil converterUtil;
    private OtpService otpService;

    // No-args constructor for Spring
    public TransactionService() {
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TransactionService(TransactionRepository repository,
                              TransactionStateHistoryRepository stateHistoryRepository,
                              TransactionValidationService validationService,
                              TransactionLimitService limitService,
                              FeeService feeService,
                              TransactionAuthorizationService authorizationService,
                              ComplianceService complianceService,
                              WalletClient walletClient,
                              FeignWalletClient feignWalletClient,
                              TransactionEventPublisher transactionEventPublisher,
                              TransactionDTOMapper dtoMapper,
                              TransactionDTOConverterUtil converterUtil,
                              OtpService otpService) {
        this.repository = repository;
        this.stateHistoryRepository = stateHistoryRepository;
        this.validationService = validationService;
        this.limitService = limitService;
        this.feeService = feeService;
        this.authorizationService = authorizationService;
        this.complianceService = complianceService;
        this.walletClient = walletClient;
        this.feignWalletClient = feignWalletClient;
        this.transactionEventPublisher = transactionEventPublisher;
        this.dtoMapper = dtoMapper;
        this.converterUtil = converterUtil;
        this.otpService = otpService;
    }

    // Backwards-compatible constructor used by tests or code that provides FeignWalletClient but not EventPublisher
    public TransactionService(TransactionRepository repository,
                              TransactionStateHistoryRepository stateHistoryRepository,
                              TransactionValidationService validationService,
                              TransactionLimitService limitService,
                              FeeService feeService,
                              TransactionAuthorizationService authorizationService,
                              ComplianceService complianceService,
                              LimitProperties limitProperties,
                              FeeProperties feeProperties,
                              WalletClient walletClient,
                              FeignWalletClient feignWalletClient) {
        this(repository, stateHistoryRepository, validationService, limitService, feeService,
                authorizationService, complianceService, walletClient, feignWalletClient, null, null, null, null);
    }

    // Backwards-compatible constructor used by tests or code that provides FeignWalletClient but not EventPublisher
    public TransactionService(TransactionRepository repository,
                              TransactionStateHistoryRepository stateHistoryRepository,
                              TransactionValidationService validationService,
                              TransactionLimitService limitService,
                              FeeService feeService,
                              TransactionAuthorizationService authorizationService,
                              ComplianceService complianceService,
                              WalletClient walletClient,
                              FeignWalletClient feignWalletClient) {
        this(repository, stateHistoryRepository, validationService, limitService, feeService,
                authorizationService, complianceService, walletClient, feignWalletClient, null, null, null, null);
    }

    // Backwards-compatible constructor used by tests or code that doesn't provide FeignWalletClient or EventPublisher
    public TransactionService(TransactionRepository repository,
                              TransactionStateHistoryRepository stateHistoryRepository,
                              TransactionValidationService validationService,
                              TransactionLimitService limitService,
                              FeeService feeService,
                              TransactionAuthorizationService authorizationService,
                              ComplianceService complianceService,
                              WalletClient walletClient) {
        this(repository, stateHistoryRepository, validationService, limitService, feeService,
                authorizationService, complianceService, walletClient, null, null, null, null, null);
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
                .authorizationMethod(evaluation.isAuthorizationRequired() ? evaluation.getMethod() : AuthorizationMethod.NONE)
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
        Transaction transaction = getTransaction(transactionId);
        if (transaction == null) {
            return null;
        }
        AuthorizationRequest authRequest = authorizationService.getLatestAuthorization(transactionId);
        String challengeCode = authRequest != null ? authRequest.getChallengeCode() : null;
        
        return AuthorizationInfoResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus())
                .authorizationMethod(transaction.getAuthorizationMethod())
                .authorizationRequired(transaction.getAuthorizationRequired())
                .challengeCode(challengeCode)
                .pendingAt(transaction.getPendingAt())
                .authorizedAt(transaction.getAuthorizedAt())
                .build();
    }

    @Transactional
    public Transaction authorizeTransaction(Long id, AuthorizationValidationRequest request) {
        Transaction tx = getTransaction(id);
        if (!Boolean.TRUE.equals(tx.getAuthorizationRequired())) {
            throw new IllegalStateException("Transaction does not require authorization");
        }
        complianceService.assertNotBlocked(tx);
        
        // Verify OTP using phone number and OTP code
        // This is the primary validation mechanism for transaction authorization
        otpService.verifyOtp(request.getPhoneNumber(), request.getOtpCode());
        
        // Get authenticated user email from SecurityContext
        String authenticatedUserEmail = request.getPhoneNumber(); // Use phone as identifier
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.zaphira.transaction.security.AuthenticatedUser) {
            com.zaphira.transaction.security.AuthenticatedUser user = (com.zaphira.transaction.security.AuthenticatedUser) auth.getPrincipal();
            authenticatedUserEmail = user.getEmail();
        }
        
        // Record authorization details in transaction audit trail
        tx.setPhoneNumberUsedForAuth(request.getPhoneNumber());
        tx.setActualAuthorizationMethod(request.getMethod().toString());
        
        // Approve authorization with OTP method
        authorizationService.approveAuthorization(id, request.getMethod(), request.getOtpCode(), authenticatedUserEmail);
        changeStatus(tx, TransactionStatus.AUTHORIZED, authenticatedUserEmail, "Authorization approved via OTP");
        return processImmediateTransaction(tx, authenticatedUserEmail);
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
            
            // Publish TransactionCompletedEvent to Kafka for asynchronous balance update
            publishTransactionCompletedEvent(transaction);
            
        } catch (Exception ex) {
            changeStatus(transaction, TransactionStatus.FAILED, actor, ex.getMessage());
            throw ex;
        }
        return transaction;
    }

    /**
     * Publie un événement TransactionCompletedEvent sur Kafka.
     * Cet événement sera consommé par user-service pour mettre à jour le solde de l'utilisateur initiateur.
     * 
     * @param transaction La transaction complétée
     */
    private void publishTransactionCompletedEvent(Transaction transaction) {
        try {
            TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                    .transactionId(transaction.getId())
                    .reference(transaction.getReference())
                    .initiatorUserId(getUserIdFromSenderWallet(transaction.getSenderWalletNumber()))
                    .senderWalletNumber(transaction.getSenderWalletNumber())
                    .receiverWalletNumber(transaction.getReceiverWalletNumber())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .feeAmount(transaction.getFeeAmount())
                    .status(transaction.getStatus().toString())
                    .completedAt(transaction.getCompletedAt())
                    .eventTimestamp(LocalDateTime.now())
                    .build();
            
            transactionEventPublisher.publishTransactionCompleted(event);
        } catch (Exception e) {
            // Log but don't fail the transaction if event publishing fails
            // The transaction is already completed in the database
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Failed to publish TransactionCompletedEvent for transaction {}: {}",
                            transaction.getId(), e.getMessage());
        }
    }

    /**
     * Récupère l'ID utilisateur correspondant à un numéro de portefeuille.
     * Utilise le service Wallet via Feign pour obtenir les informations.
     *
     * @param walletNumber Le numéro de portefeuille
     * @return L'ID utilisateur ou null si non trouvé
     */
    private Long getUserIdFromSenderWallet(String walletNumber) {
        try {
            WalletDTO walletDTO = feignWalletClient.getWalletByNumber(walletNumber);
            return walletDTO != null ? walletDTO.getUserId() : null;
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Failed to get user ID from wallet {}: {}", walletNumber, e.getMessage());
            return null;
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
                .balance(walletDto.getBalance())
                .active(walletDto.getActive())
                .userId(walletDto.getUserId())
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

    // ====== DTO CONVERSION METHODS ======

    /**
     * Convertit une entité Transaction en TransactionDTO
     * Évite les problèmes de LazyInitializationException
     * @param transaction l'entité à convertir
     * @return le DTO correspondant
     */
    public TransactionDTO toTransactionDTO(Transaction transaction) {
        if (dtoMapper == null) {
            return null;
        }
        return dtoMapper.toDTO(transaction);
    }

    /**
     * Convertit une liste d'entités Transaction en liste de DTOs
     * @param transactions les entités à convertir
     * @return la liste de DTOs correspondants
     */
    public List<TransactionDTO> toTransactionDTOList(List<Transaction> transactions) {
        if (converterUtil == null) {
            return null;
        }
        return converterUtil.toDTOList(transactions);
    }

    /**
     * Convertit une entité TransactionStateHistory en TransactionStateHistoryDTO
     * @param history l'entité à convertir
     * @return le DTO correspondant
     */
    public TransactionStateHistoryDTO toStateHistoryDTO(TransactionStateHistory history) {
        if (dtoMapper == null) {
            return null;
        }
        return dtoMapper.toDTO(history);
    }

    /**
     * Convertit une liste d'entités TransactionStateHistory en liste de DTOs
     * @param histories les entités à convertir
     * @return la liste de DTOs correspondants
     */
    public List<TransactionStateHistoryDTO> toStateHistoryDTOList(List<TransactionStateHistory> histories) {
        if (converterUtil == null) {
            return null;
        }
        return converterUtil.toHistoryDTOList(histories);
    }

}

