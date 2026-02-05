package com.zaphira.wallet.service;

import com.zaphira.common.dto.WalletSummaryDTO;
import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.dto.TransferRequest;
import com.zaphira.wallet.dto.request.*;
import com.zaphira.wallet.dto.response.CreateWalletResponse;
import com.zaphira.wallet.dto.response.TransactionValidationResponse;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

   // WalletDTO createUserWallet(CreateWalletRequest request);
   CreateWalletResponse createWalletForUser(CreateWalletRequest request);

    CreateWalletResponse createWalletForMerchant(CreateMerchantWalletRequest request);
//    WalletDTO createWalletForUser(Long userId);

    // Consultation
    WalletDTO getWalletByNumber(String walletNumber);

   //
   @Transactional(readOnly = true)
   WalletDTO getWalletById(Long id);

   @Transactional(readOnly = true)
   List<WalletDTO> getUserWallets(Long userId);

   WalletSummaryDTO getWalletSummary(Long userId);

   com.zaphira.common.dto.WalletSummaryDTO getWalletSummaryByWalletNumber(String walletNumber);

    WalletDTO freezeWallet(String walletNumber, FreezeWalletRequest request);

    WalletDTO unfreezeWallet(String walletNumber, String unfrozenBy, String notes);

    WalletDTO suspendWallet(String walletNumber, String reason, String suspendedBy);

    WalletDTO activateWallet(String walletNumber, String activatedBy);

    WalletDTO closeWallet(String walletNumber, String closedBy, String reason);

    WalletDTO creditWallet(Long walletId, BalanceOperationRequest request);

    WalletDTO debitWallet(Long walletId, BalanceOperationRequest request);

    WalletDTO blockAmount(Long walletId, BalanceOperationRequest request);

    WalletDTO unblockAmount(Long walletId, BalanceOperationRequest request);

    WalletDTO releaseBlockedAmount(Long walletId, BalanceOperationRequest request);

    void transfer(TransferRequest request);

    TransactionValidationResponse validateTransaction(TransactionValidationRequest request);

    void resetDailyLimits();

    void resetMonthlyLimits();

    WalletDTO updateLimits(String walletNumber, BigDecimal dailyLimit, BigDecimal monthlyLimit);

    @Transactional(readOnly = true)
    boolean hasAvailableBalance(String walletNumber, BigDecimal amount);

//    WalletIdResponseLo getWalletIdByWalletNumber (String walletNumber);

    void recalculateBalance(String walletNumber);
//    WalletDTO getWalletById(Long id);
//    List<WalletDTO> getUserWallets(Long userId);
//    WalletDTO getPrimaryWallet(Long userId);
//    WalletSummaryDTO getWalletSummary(Long userId);
//
//    // Gestion du statut
//    WalletDTO freezeWallet(String walletNumber, FreezeWalletRequest request);
//    WalletDTO unfreezeWallet(String walletNumber, String unfrozenBy, String notes);
//    WalletDTO suspendWallet(String walletNumber, String reason, String suspendedBy);
//    WalletDTO activateWallet(String walletNumber, String activatedBy);
//    WalletDTO closeWallet(String walletNumber, String closedBy, String reason);
//
//    // Gestion des soldes
//    WalletDTO creditWallet(String walletNumber, BalanceOperationRequest request);
//    WalletDTO debitWallet(String walletNumber, BalanceOperationRequest request);
//    WalletDTO blockAmount(String walletNumber, BalanceOperationRequest request);
//    WalletDTO unblockAmount(String walletNumber, BalanceOperationRequest request);
//    WalletDTO releaseBlockedAmount(String walletNumber, BalanceOperationRequest request);
//
//    // Validation de transaction
//    TransactionValidationResponse validateTransaction(TransactionValidationRequest request);
//
//    // Gestion des limites
//    void resetDailyLimits();
//    void resetMonthlyLimits();
//    WalletDTO updateLimits(String walletNumber, BigDecimal dailyLimit, BigDecimal monthlyLimit);
//
//    // Utilitaires
//    boolean hasAvailableBalance(String walletNumber, BigDecimal amount);
//    void recalculateBalance(String walletNumber);
//
//    // Création
//    WalletDTO createWallet(CreateWalletRequest request, Long requestingUserId);
//
//    // Consultation (avec vérification de permissions)
//    WalletDTO getWallet(String walletNumber, Long requestingUserId);
//    List<WalletDTO> getAccessibleWallets(Long userId); // Wallets où user a des permissions
//
//    // Gestion du statut (avec vérification de permissions)
//    WalletDTO freezeWallet(String walletNumber, FreezeWalletRequest request, Long requestingUserId);
//    WalletDTO unfreezeWallet(String walletNumber, String unfrozenBy, String notes, Long requestingUserId);
//
//    // ... autres méthodes
//
//    // Nouvelles méthodes pour hiérarchie
//    WalletDTO transferBetweenSubWallets(TransferBetweenSubWalletsRequest request, Long requestingUserId);
//    BalanceSummaryDTO getConsolidatedBalance(String walletNumber, Long requestingUserId);
}