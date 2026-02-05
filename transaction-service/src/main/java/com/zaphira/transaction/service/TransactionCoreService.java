package com.zaphira.transaction.service;

import com.zaphira.common.dto.WalletDTO;
import com.zaphira.common.dto.request.BalanceOperationRequest;
import com.zaphira.common.model.enums.Currency;
import com.zaphira.transaction.client.WalletServiceClient;
import com.zaphira.transaction.dto.core.CreateTransactionCoreRequest;
import com.zaphira.transaction.dto.core.TransactionCoreDTO;
import com.zaphira.transaction.exception.TransactionExceptions;
import com.zaphira.transaction.mapper.TransactionCoreMapper;
import com.zaphira.transaction.model.core.TransactionCore;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.repository.TransactionCoreRepository;
import com.zaphira.transaction.util.TransactionReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionCoreService - LOT 1 + LOT 2 Service
 * 
 * LOT 1: Basic operations
 * - P2P Transfer, Deposit, Withdrawal
 * 
 * LOT 2: Extended operations
 * - Merchant Payment
 * - Fee calculation
 * - Cancel transaction
 * - Retry transaction
 * - Timeline tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCoreService {

    private final TransactionCoreRepository transactionCoreRepository;
    private final TransactionCoreMapper transactionCoreMapper;
    private final WalletServiceClient walletServiceClient;
    private final TransactionReferenceGenerator referenceGenerator;
    private final FeeCalculationService feeCalculationService;
    private final TransactionTimelineService timelineService;
    private final com.zaphira.transaction.repository.TransactionFeesRepository feesRepository;
    private final com.zaphira.transaction.repository.TransactionMetadataRepository metadataRepository;

    /* =========================
       TRANSACTION CREATION
       ========================= */

    /**
     * Create P2P Transfer
     */
    @Transactional
    public TransactionCoreDTO createTransfer(CreateTransactionCoreRequest request) {
        log.info("Creating P2P transfer from wallet {} to wallet {}", 
                request.getSenderWalletId(), request.getReceiverWalletId());

        // Validate request
        validateTransferRequest(request);

        // Check sender wallet balance
        WalletDTO senderWallet = walletServiceClient.getWallet(request.getSenderWalletId());
        if (senderWallet.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw TransactionExceptions.insufficientBalance();
        }

        // Create transaction entity
        TransactionCore transaction = TransactionCore.builder()
                .reference(referenceGenerator.generate())
                .senderWalletId(request.getSenderWalletId())
                .receiverWalletId(request.getReceiverWalletId())
                .amount(request.getAmount())
                .currency(Currency.valueOf(request.getCurrency()))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .build();

        transaction = transactionCoreRepository.save(transaction);
        log.info("Transaction created with reference: {}", transaction.getReference());

        // Execute wallet operations
        try {
            executeTransfer(transaction);
            transaction = markAsCompleted(transaction);
            log.info("Transfer completed: {}", transaction.getReference());
        } catch (Exception e) {
            transaction = markAsFailed(transaction);
            log.error("Transfer failed: {}", transaction.getReference(), e);
            throw TransactionExceptions.transactionFailed(e.getMessage());
        }

        return transactionCoreMapper.toDTO(transaction);
    }

    /**
     * Create Deposit
     */
    @Transactional
    public TransactionCoreDTO createDeposit(Long receiverWalletId, BigDecimal amount, String currency, String description) {
        log.info("Creating deposit for wallet {} amount {}", receiverWalletId, amount);

        // Validate
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw TransactionExceptions.invalidAmount();
        }

        // Create transaction
        TransactionCore transaction = TransactionCore.builder()
                .reference(referenceGenerator.generate())
                .receiverWalletId(receiverWalletId)
                .amount(amount)
                .currency(Currency.valueOf(currency))
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .description(description)
                .build();

        transaction = transactionCoreRepository.save(transaction);

        // Execute deposit
        try {
            creditWallet(transaction);
            transaction = markAsCompleted(transaction);
            log.info("Deposit completed: {}", transaction.getReference());
        } catch (Exception e) {
            transaction = markAsFailed(transaction);
            log.error("Deposit failed: {}", transaction.getReference(), e);
            throw TransactionExceptions.transactionFailed(e.getMessage());
        }

        return transactionCoreMapper.toDTO(transaction);
    }

    /**
     * Create Withdrawal
     */
    @Transactional
    public TransactionCoreDTO createWithdrawal(Long senderWalletId, BigDecimal amount, String currency, String description) {
        log.info("Creating withdrawal from wallet {} amount {}", senderWalletId, amount);

        // Validate
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw TransactionExceptions.invalidAmount();
        }

        // Check balance
        WalletDTO senderWallet = walletServiceClient.getWallet(senderWalletId);
        if (senderWallet.getAvailableBalance().compareTo(amount) < 0) {
            throw TransactionExceptions.insufficientBalance();
        }

        // Create transaction
        TransactionCore transaction = TransactionCore.builder()
                .reference(referenceGenerator.generate())
                .senderWalletId(senderWalletId)
                .amount(amount)
                .currency(Currency.valueOf(currency))
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.PENDING)
                .description(description)
                .build();

        transaction = transactionCoreRepository.save(transaction);

        // Execute withdrawal
        try {
            debitWallet(transaction);
            transaction = markAsCompleted(transaction);
            log.info("Withdrawal completed: {}", transaction.getReference());
        } catch (Exception e) {
            transaction = markAsFailed(transaction);
            log.error("Withdrawal failed: {}", transaction.getReference(), e);
            throw TransactionExceptions.transactionFailed(e.getMessage());
        }

        return transactionCoreMapper.toDTO(transaction);
    }

    /* =========================
       QUERY OPERATIONS
       ========================= */

    /**
     * Get transaction by reference
     */
    @Transactional(readOnly = true)
    public TransactionCoreDTO getByReference(String reference) {
        log.debug("Getting transaction by reference: {}", reference);
        TransactionCore transaction = transactionCoreRepository.findByReference(reference)
                .orElseThrow(() -> TransactionExceptions.transactionNotFound(reference));
        return transactionCoreMapper.toDTO(transaction);
    }

    /**
     * Get transaction by ID
     */
    @Transactional(readOnly = true)
    public TransactionCoreDTO getById(Long id) {
        log.debug("Getting transaction by ID: {}", id);
        TransactionCore transaction = transactionCoreRepository.findById(id)
                .orElseThrow(() -> TransactionExceptions.transactionNotFound(String.valueOf(id)));
        return transactionCoreMapper.toDTO(transaction);
    }

    /**
     * Get wallet transaction history
     */
    @Transactional(readOnly = true)
    public Page<TransactionCoreDTO> getWalletHistory(Long walletId, Pageable pageable) {
        log.debug("Getting transaction history for wallet: {}", walletId);
        Page<TransactionCore> transactions = transactionCoreRepository.findByWalletId(walletId, pageable);
        return transactions.map(transactionCoreMapper::toDTO);
    }

    /**
     * Get sent transactions
     */
    @Transactional(readOnly = true)
    public Page<TransactionCoreDTO> getSentTransactions(Long walletId, Pageable pageable) {
        log.debug("Getting sent transactions for wallet: {}", walletId);
        Page<TransactionCore> transactions = transactionCoreRepository
                .findBySenderWalletIdOrderByCreatedAtDesc(walletId, pageable);
        return transactions.map(transactionCoreMapper::toDTO);
    }

    /**
     * Get received transactions
     */
    @Transactional(readOnly = true)
    public Page<TransactionCoreDTO> getReceivedTransactions(Long walletId, Pageable pageable) {
        log.debug("Getting received transactions for wallet: {}", walletId);
        Page<TransactionCore> transactions = transactionCoreRepository
                .findByReceiverWalletIdOrderByCreatedAtDesc(walletId, pageable);
        return transactions.map(transactionCoreMapper::toDTO);
    }

    /**
     * Get transactions by type
     */
    @Transactional(readOnly = true)
    public Page<TransactionCoreDTO> getByType(TransactionType type, Pageable pageable) {
        log.debug("Getting transactions by type: {}", type);
        Page<TransactionCore> transactions = transactionCoreRepository
                .findByTypeOrderByCreatedAtDesc(type, pageable);
        return transactions.map(transactionCoreMapper::toDTO);
    }

    /* =========================
       PRIVATE HELPER METHODS
       ========================= */

    private void validateTransferRequest(CreateTransactionCoreRequest request) {
        if (request.getSenderWalletId().equals(request.getReceiverWalletId())) {
            throw TransactionExceptions.sameWalletTransfer();
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw TransactionExceptions.invalidAmount();
        }
    }

    private void executeTransfer(TransactionCore transaction) {
        // Debit sender
        BalanceOperationRequest debitRequest = BalanceOperationRequest.builder()
                .amount(transaction.getAmount())
                .transactionReference(transaction.getReference())
                .description("Transfer to wallet " + transaction.getReceiverWalletId())
                .build();

        walletServiceClient.debitWallet(transaction.getSenderWalletId(), debitRequest);
        log.debug("Debited wallet {}", transaction.getSenderWalletId());

        // Credit receiver
        BalanceOperationRequest creditRequest = BalanceOperationRequest.builder()
                .amount(transaction.getAmount())
                .transactionReference(transaction.getReference())
                .description("Transfer from wallet " + transaction.getSenderWalletId())
                .build();

        walletServiceClient.creditWallet(transaction.getReceiverWalletId(), creditRequest);
        log.debug("Credited wallet {}", transaction.getReceiverWalletId());
    }

    private void creditWallet(TransactionCore transaction) {
        BalanceOperationRequest request = BalanceOperationRequest.builder()
                .amount(transaction.getAmount())
                .transactionReference(transaction.getReference())
                .description(transaction.getDescription() != null ? transaction.getDescription() : "Deposit")
                .build();

        walletServiceClient.creditWallet(transaction.getReceiverWalletId(), request);
        log.debug("Credited wallet {}", transaction.getReceiverWalletId());
    }

    private void debitWallet(TransactionCore transaction) {
        BalanceOperationRequest request = BalanceOperationRequest.builder()
                .amount(transaction.getAmount())
                .transactionReference(transaction.getReference())
                .description(transaction.getDescription() != null ? transaction.getDescription() : "Withdrawal")
                .build();

        walletServiceClient.debitWallet(transaction.getSenderWalletId(), request);
        log.debug("Debited wallet {}", transaction.getSenderWalletId());
    }

    private TransactionCore markAsCompleted(TransactionCore transaction) {
        TransactionStatus previousStatus = transaction.getStatus();
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        TransactionCore saved = transactionCoreRepository.save(transaction);
        
        // Record timeline
        timelineService.recordStatusChange(saved, previousStatus, TransactionStatus.COMPLETED, "Transaction completed successfully");
        
        return saved;
    }

    private TransactionCore markAsFailed(TransactionCore transaction) {
        TransactionStatus previousStatus = transaction.getStatus();
        transaction.setStatus(TransactionStatus.FAILED);
        TransactionCore saved = transactionCoreRepository.save(transaction);
        
        // Record timeline
        timelineService.recordStatusChange(saved, previousStatus, TransactionStatus.FAILED, "Transaction failed");
        
        return saved;
    }

    /* =========================
       LOT 2: NEW OPERATIONS
       ========================= */

    /**
     * Create Merchant Payment (LOT 2)
     * Charges merchant fee (2%) + platform fee (0.5%) = 2.5% total
     */
    @Transactional
    public TransactionCoreDTO createMerchantPayment(Long senderWalletId, Long merchantWalletId, BigDecimal amount, String currency, String description) {
        log.info("Creating merchant payment from wallet {} to merchant {} amount {}", senderWalletId, merchantWalletId, amount);

        // Validate
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw TransactionExceptions.invalidAmount();
        }

        // Create transaction
        TransactionCore transaction = TransactionCore.builder()
                .reference(referenceGenerator.generate())
                .senderWalletId(senderWalletId)
                .receiverWalletId(merchantWalletId)
                .amount(amount)
                .currency(Currency.valueOf(currency))
                .type(TransactionType.MERCHANT_PAYMENT)
                .status(TransactionStatus.PENDING)
                .description(description)
                .build();

        // Calculate fees
        com.zaphira.transaction.model.core.TransactionFees fees = feeCalculationService.calculateFees(transaction);
        transaction.setFeeAmount(fees.getTotalFees());
        transaction.calculateTotalAmount();
        transaction.setFees(fees);

        // Check sender balance (amount + fees)
        WalletDTO senderWallet = walletServiceClient.getWallet(senderWalletId);
        if (senderWallet.getAvailableBalance().compareTo(transaction.getTotalAmount()) < 0) {
            throw TransactionExceptions.insufficientBalance();
        }

        transaction = transactionCoreRepository.save(transaction);
        feesRepository.save(fees);
        
        // Record timeline
        timelineService.recordStatusChange(transaction, null, TransactionStatus.PENDING, "Merchant payment created");

        // Execute payment
        try {
            executeMerchantPayment(transaction);
            transaction = markAsCompleted(transaction);
            log.info("Merchant payment completed: {}", transaction.getReference());
        } catch (Exception e) {
            transaction.setFailureReason(e.getMessage());
            transaction = markAsFailed(transaction);
            log.error("Merchant payment failed: {}", transaction.getReference(), e);
            throw TransactionExceptions.transactionFailed(e.getMessage());
        }

        return transactionCoreMapper.toDTO(transaction);
    }

    /**
     * Cancel Transaction (LOT 2)
     * Only PENDING transactions can be cancelled
     */
    @Transactional
    public TransactionCoreDTO cancelTransaction(String reference) {
        log.info("Cancelling transaction: {}", reference);

        TransactionCore transaction = transactionCoreRepository.findByReference(reference)
                .orElseThrow(() -> TransactionExceptions.transactionNotFound(reference));

        if (!transaction.canBeCancelled()) {
            throw TransactionExceptions.cannotCancelTransaction(transaction.getStatus());
        }

        TransactionStatus previousStatus = transaction.getStatus();
        transaction.markCancelled();
        transaction = transactionCoreRepository.save(transaction);

        // Record timeline
        timelineService.recordStatusChange(transaction, previousStatus, TransactionStatus.CANCELLED, "Transaction cancelled by user");

        log.info("Transaction cancelled: {}", reference);
        return transactionCoreMapper.toDTO(transaction);
    }

    /**
     * Retry Failed Transaction (LOT 2)
     * Only FAILED transactions with retry count < max can be retried
     */
    @Transactional
    public TransactionCoreDTO retryTransaction(String reference) {
        log.info("Retrying transaction: {}", reference);

        TransactionCore transaction = transactionCoreRepository.findByReference(reference)
                .orElseThrow(() -> TransactionExceptions.transactionNotFound(reference));

        if (!transaction.canBeRetried()) {
            throw TransactionExceptions.cannotRetryTransaction(transaction.getStatus());
        }

        // Increment retry count
        transaction.getMetadata().incrementRetryCount();
        metadataRepository.save(transaction.getMetadata());

        // Reset to PENDING
        TransactionStatus previousStatus = transaction.getStatus();
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCompletedAt(null);
        transaction = transactionCoreRepository.save(transaction);

        // Record timeline
        timelineService.recordStatusChange(transaction, previousStatus, TransactionStatus.PENDING, 
                "Transaction retry attempt " + transaction.getMetadata().getRetryCount());

        // Execute based on type
        try {
            switch (transaction.getType()) {
                case TRANSFER:
                    executeTransfer(transaction);
                    break;
                case MERCHANT_PAYMENT:
                    executeMerchantPayment(transaction);
                    break;
                case DEPOSIT:
                    creditWallet(transaction);
                    break;
                case WITHDRAWAL:
                    debitWallet(transaction);
                    break;
                default:
                    throw new IllegalArgumentException("Cannot retry transaction type: " + transaction.getType());
            }

            transaction = markAsCompleted(transaction);
            log.info("Transaction retry succeeded: {}", reference);
        } catch (Exception e) {
            transaction.setFailureReason("Retry failed: " + e.getMessage());
            transaction = markAsFailed(transaction);
            log.error("Transaction retry failed: {}", reference, e);
            throw TransactionExceptions.transactionFailed(e.getMessage());
        }

        return transactionCoreMapper.toDTO(transaction);
    }

    /**
     * Execute merchant payment with fees
     */
    private void executeMerchantPayment(TransactionCore transaction) {
        BigDecimal totalAmount = transaction.getTotalAmount();
        BigDecimal merchantReceives = transaction.getAmount().subtract(transaction.getFees().getMerchantFee());

        // Debit sender (amount + platform fee)
        BalanceOperationRequest debitRequest = BalanceOperationRequest.builder()
                .amount(totalAmount)
                .transactionReference(transaction.getReference())
                .description("Merchant payment to wallet " + transaction.getReceiverWalletId())
                .build();

        walletServiceClient.debitWallet(transaction.getSenderWalletId(), debitRequest);
        log.debug("Debited wallet {} amount {}", transaction.getSenderWalletId(), totalAmount);

        // Credit merchant (amount - merchant fee)
        BalanceOperationRequest creditRequest = BalanceOperationRequest.builder()
                .amount(merchantReceives)
                .transactionReference(transaction.getReference())
                .description("Merchant payment from wallet " + transaction.getSenderWalletId())
                .build();

        walletServiceClient.creditWallet(transaction.getReceiverWalletId(), creditRequest);
        log.debug("Credited merchant wallet {} amount {}", transaction.getReceiverWalletId(), merchantReceives);
    }

    /* =========================
       STATE MANAGEMENT - REVERSE & REFUND
       ========================= */

    /**
     * Reverse a completed transaction
     * Creates a new transaction in the opposite direction
     * ADMIN only
     */
    @Transactional
    public TransactionCoreDTO reverseTransaction(String reference, String reason) {
        log.info("Reversing transaction: {} - Reason: {}", reference, reason);

        // Find original transaction
        TransactionCore original = transactionCoreRepository.findByReference(reference)
                .orElseThrow(() -> TransactionExceptions.transactionNotFound(reference));

        // Validate can be reversed
        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw TransactionExceptions.invalidTransactionState(
                    "Only COMPLETED transactions can be reversed. Current state: " + original.getStatus());
        }

        if (original.getStatus() == TransactionStatus.REVERSED) {
            throw TransactionExceptions.transactionAlreadyReversed(reference);
        }

        // Create reverse transaction
        TransactionCore reverseTransaction = TransactionCore.builder()
                .reference(referenceGenerator.generate())
                .senderWalletId(original.getReceiverWalletId())  // Swap sender/receiver
                .receiverWalletId(original.getSenderWalletId())
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .type(TransactionType.REVERSAL)
                .status(TransactionStatus.PENDING)
                .description("Reversal of transaction " + reference + ". Reason: " + reason)
                .build();

        reverseTransaction = transactionCoreRepository.save(reverseTransaction);
        log.info("Reverse transaction created: {}", reverseTransaction.getReference());

        // Execute reverse transfer
        try {
            executeTransfer(reverseTransaction);
            reverseTransaction = markAsCompleted(reverseTransaction);
            
            // Update original transaction status
            original.setStatus(TransactionStatus.REVERSED);
            original.setUpdatedAt(LocalDateTime.now());
            transactionCoreRepository.save(original);
            
            log.info("Transaction reversed successfully: {} -> Reverse: {}", 
                    reference, reverseTransaction.getReference());
        } catch (Exception e) {
            reverseTransaction = markAsFailed(reverseTransaction);
            log.error("Reverse transaction failed: {}", reverseTransaction.getReference(), e);
            throw TransactionExceptions.transactionFailed("Reverse failed: " + e.getMessage());
        }

        return transactionCoreMapper.toDTO(reverseTransaction);
    }

    /**
     * Refund a completed transaction
     * Creates a refund transaction returning funds to original sender
     * ADMIN or merchant who received payment can refund
     */
    @Transactional
    public TransactionCoreDTO refundTransaction(String reference, String reason) {
        log.info("Refunding transaction: {} - Reason: {}", reference, reason);

        // Find original transaction
        TransactionCore original = transactionCoreRepository.findByReference(reference)
                .orElseThrow(() -> TransactionExceptions.transactionNotFound(reference));

        // Validate can be refunded
        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw TransactionExceptions.invalidTransactionState(
                    "Only COMPLETED transactions can be refunded. Current state: " + original.getStatus());
        }

        if (original.getStatus() == TransactionStatus.REFUNDED) {
            throw TransactionExceptions.transactionAlreadyRefunded(reference);
        }

        // Create refund transaction
        TransactionCore refundTransaction = TransactionCore.builder()
                .reference(referenceGenerator.generate())
                .senderWalletId(original.getReceiverWalletId())  // Refund from receiver
                .receiverWalletId(original.getSenderWalletId())  // Back to original sender
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .type(TransactionType.REFUND)
                .status(TransactionStatus.PENDING)
                .description("Refund of transaction " + reference + ". Reason: " + reason)
                .build();

        refundTransaction = transactionCoreRepository.save(refundTransaction);
        log.info("Refund transaction created: {}", refundTransaction.getReference());

        // Execute refund transfer
        try {
            executeTransfer(refundTransaction);
            refundTransaction = markAsCompleted(refundTransaction);
            
            // Update original transaction status
            original.setStatus(TransactionStatus.REFUNDED);
            original.setUpdatedAt(LocalDateTime.now());
            transactionCoreRepository.save(original);
            
            log.info("Transaction refunded successfully: {} -> Refund: {}", 
                    reference, refundTransaction.getReference());
        } catch (Exception e) {
            refundTransaction = markAsFailed(refundTransaction);
            log.error("Refund transaction failed: {}", refundTransaction.getReference(), e);
            throw TransactionExceptions.transactionFailed("Refund failed: " + e.getMessage());
        }

        return transactionCoreMapper.toDTO(refundTransaction);
    }
}
