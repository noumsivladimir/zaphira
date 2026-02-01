package com.zaphira.transaction.service;

import com.zaphira.common.model.enums.PermissionType;
import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.TransactionSummaryDTO;
import com.zaphira.transaction.dto.requests.CreateTransactionRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PathVariable;

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

    Boolean hasPermissionTo(PermissionType permissionType, @PathVariable("walletId") Long walletId);

    // Gestion du statut
    TransactionDTO processTransaction(String transactionReference);
    TransactionDTO completeTransaction(String transactionReference);
    TransactionDTO failTransaction(String transactionReference, String reason);
    TransactionDTO cancelTransaction(String transactionReference, String reason);
//    TransactionDTO reverseTransaction(String transactionReference, String reason);

    // Reporting
    TransactionSummaryDTO getTransactionSummary(Long userId, LocalDate startDate, LocalDate endDate);
//    List<TransactionDTO> getPendingTransactions();

    // Utilitaires
//    boolean existsByReference(String transactionReference);
//    void retryFailedTransaction(String transactionReference);
}
