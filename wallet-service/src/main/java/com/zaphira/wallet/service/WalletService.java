package com.zaphira.wallet.service;

import com.zaphira.wallet.dto.request.CreateWalletRequest;
import com.zaphira.wallet.dto.response.CreateWalletResponse;

public interface WalletService {

   // WalletDTO createUserWallet(CreateWalletRequest request);
   CreateWalletResponse createWalletForUser(CreateWalletRequest request);
//   WalletDTO createWalletForMerchant(CreateWalletRequest request);
//    WalletDTO createWalletForUser(Long userId);

    // Consultation
//    WalletDTO getWalletByNumber(String walletNumber);
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