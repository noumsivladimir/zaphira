package com.zaphira.transaction.service;

import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.TransactionSummaryDTO;
import com.zaphira.transaction.dto.requests.CreateTransactionRequest;
import com.zaphira.transaction.dto.requests.TransferRequest;
import com.zaphira.transaction.dto.requests.PaymentRequest;
import com.zaphira.transaction.dto.requests.BulkTransferRequest;
import com.zaphira.transaction.dto.requests.SplitPaymentRequest;
import com.zaphira.transaction.dto.response.SplitPaymentResponse;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.math.BigDecimal;

public interface TransactionService {


    TransactionDTO createTransaction(CreateTransactionRequest request);
    TransactionDTO createTransfer(TransferRequest request);
    TransactionDTO createDeposit(TransferRequest request);
    TransactionDTO createWithdrawal(TransferRequest request);
    TransactionDTO createMerchantPayment(PaymentRequest request);
    Page<TransactionDTO> createBulkTransfer(BulkTransferRequest request);
    SplitPaymentResponse createSplitPayment(SplitPaymentRequest request); // LOT 2: Split Payment
//    TransactionDTO initiateTransfer(TransferRequest request);
//    TransactionDTO initiateWithdrawal(WithdrawalRequest request);
//    TransactionDTO initiateDeposit(DepositRequest request);
//    Boolean initiateTransfer(TransferRequest request);

    // Consultation
    TransactionDTO getTransaction(String transactionReference);
    TransactionDTO getTransactionById(Long id);
    Page<TransactionDTO> getUserTransactions(Long userId, int page, int size);
    Page<TransactionDTO> getWalletTransactions(String walletNumber, int page, int size);
    Page<TransactionDTO> searchTransactions(
            TransactionStatus status,
            TransactionType type,
            String currency,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDate from,
            LocalDate to,
            String reference,
            int page,
            int size);

    // Gestion du statut
    TransactionDTO processTransaction(String transactionReference);
    TransactionDTO completeTransaction(String transactionReference);
    TransactionDTO failTransaction(String transactionReference, String reason);
    TransactionDTO cancelTransaction(String transactionReference, String reason);
    TransactionDTO reverseTransaction(String transactionReference, String reason);
    TransactionDTO refundTransaction(String transactionReference, String reason);
    TransactionDTO retryFailedTransaction(String transactionReference);

    // Reporting
    TransactionSummaryDTO getTransactionSummary(Long userId, LocalDate startDate, LocalDate endDate);
//    List<TransactionDTO> getPendingTransactions();

    // LOT 4: User-scoped Search
    Page<TransactionDTO> getMyTransactions(TransactionStatus status, TransactionType type,
                                            LocalDate from, LocalDate to, int page, int size);
    Page<TransactionDTO> getMySentTransactions(TransactionStatus status, int page, int size);
    Page<TransactionDTO> getMyReceivedTransactions(TransactionStatus status, int page, int size);

    // Utilitaires
//    boolean existsByReference(String transactionReference);
//    void retryFailedTransaction(String transactionReference);
}