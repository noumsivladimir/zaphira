package com.zaphira.wallet.service;

import com.zaphira.common.dto.WalletSummaryDTO;
import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.dto.request.*;
import com.zaphira.wallet.dto.response.CreateWalletResponse;
import com.zaphira.wallet.dto.response.TransactionValidationResponse;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public interface WalletService {

   // WalletDTO createUserWallet(CreateWalletRequest request);
   CreateWalletResponse createWalletForUser(CreateWalletRequest request);

    public WalletDTO createWallet(Long userId) {
        // Générer un numéro de portefeuille unique à 8 chiffres
        String walletNumber = generateUniqueWalletNumber();

   //
   @Transactional(readOnly = true)
   WalletDTO getWalletById(Long id);

   WalletSummaryDTO getWalletSummary(Long userId);

   com.zaphira.common.dto.WalletSummaryDTO getWalletSummaryByWalletNumber(String walletNumber);

    /**
     * Génère un numéro de portefeuille unique à 8 chiffres
     * Utilise une approche aléatoire avec vérification d'unicité en base
     */
    private String generateUniqueWalletNumber() {
        Random random = new Random();
        String walletNumber;
        int maxAttempts = 100; // Éviter une boucle infinie
        int attempts = 0;

        do {
            // Générer un nombre aléatoire entre 10000000 et 99999999
            int number = 10000000 + random.nextInt(90000000);
            walletNumber = String.format("%08d", number);
            attempts++;

            if (attempts >= maxAttempts) {
                throw new RuntimeException("Impossible de générer un numéro de portefeuille unique après " + maxAttempts + " tentatives");
            }
        } while (walletRepository.existsByWalletNumber(walletNumber));

        return walletNumber;
    }

    public WalletDTO getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId));
        return toDTO(wallet);
    }

    WalletDTO unfreezeWallet(String walletNumber, String unfrozenBy, String notes);

    WalletDTO suspendWallet(String walletNumber, String reason, String suspendedBy);

    WalletDTO activateWallet(String walletNumber, String activatedBy);

    WalletDTO closeWallet(String walletNumber, String closedBy, String reason);

    WalletDTO creditWallet(Long walletId, BalanceOperationRequest request);

    WalletDTO debitWallet(Long walletId, BalanceOperationRequest request);

    WalletDTO blockAmount(Long walletId, BalanceOperationRequest request);

    WalletDTO unblockAmount(Long walletId, BalanceOperationRequest request);

    WalletDTO releaseBlockedAmount(Long walletId, BalanceOperationRequest request);

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