package com.zaphira.wallet.service;

import com.zaphira.common.dto.WalletDTO;
import com.zaphira.common.exception.ResourceNotFoundException;
//import com.zaphira.wallet.client.TransactionServiceClient;
import com.zaphira.common.model.entities.Wallet;
import com.zaphira.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    //private final TransactionServiceClient transactionServiceClient;

    public WalletDTO createWallet(Long userId) {
        // Générer un numéro de portefeuille unique à 8 chiffres
        String walletNumber = generateUniqueWalletNumber();

        // Créer le wallet avec walletNumber déjà défini
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .availableBalance(BigDecimal.ZERO)
                .blockedBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .active(true)
                .walletNumber(walletNumber)
                .currency("XOF")  // Default currency for Cameroon
                .status("ACTIVE")  // Default status
                .type("USER")  // Default type for regular users
                .dailySpent(BigDecimal.ZERO)
                .monthlySpent(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Sauvegarder le wallet en base
        Wallet saved = walletRepository.save(wallet);

        return toDTO(saved);
    }

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

    public WalletDTO getWalletByNumber(String walletNumber) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet with number " + walletNumber));
        return toDTO(wallet);
    }

    @Transactional
    public void debit(String walletNumber, BigDecimal amount) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet with number " + walletNumber));

        // Vérifier si le wallet est gelé
        if (wallet.getFrozenAt() != null) {
            throw new RuntimeException("Wallet is frozen and cannot be debited");
        }

        // Vérifier les limites quotidiennes
        checkAndUpdateDailyLimits(wallet, amount);

        // Vérifier le solde disponible
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient available balance");
        }

        // Mettre à jour les soldes en maintenant la cohérence
        BigDecimal newAvailableBalance = wallet.getAvailableBalance().subtract(amount);
        BigDecimal blockedBalance = wallet.getBlockedBalance() != null ? wallet.getBlockedBalance() : BigDecimal.ZERO;
        BigDecimal newTotalBalance = newAvailableBalance.add(blockedBalance);

        wallet.setAvailableBalance(newAvailableBalance);
        wallet.setTotalBalance(newTotalBalance);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);
    }

    @Transactional
    public void credit(String walletNumber, BigDecimal amount) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet with number " + walletNumber));

        // Créditer le wallet en maintenant la cohérence
        BigDecimal newAvailableBalance = wallet.getAvailableBalance().add(amount);
        BigDecimal blockedBalance = wallet.getBlockedBalance() != null ? wallet.getBlockedBalance() : BigDecimal.ZERO;
        BigDecimal newTotalBalance = newAvailableBalance.add(blockedBalance);

        wallet.setAvailableBalance(newAvailableBalance);
        wallet.setTotalBalance(newTotalBalance);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);
    }

    @Transactional
    public void transfer(String senderWalletNumber, String receiverWalletNumber, BigDecimal amount) {
        debit(senderWalletNumber, amount);
        credit(receiverWalletNumber, amount);
    }

    /**
     * Bloque des fonds sur un wallet (les rend indisponibles pour les transactions)
     */
    @Transactional
    public void blockFunds(String walletNumber, BigDecimal amount) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet with number " + walletNumber));

        // Vérifier que le solde disponible est suffisant
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient available balance to block funds");
        }

        // Bloquer les fonds : available -= amount, blocked += amount, total reste inchangé
        BigDecimal newAvailableBalance = wallet.getAvailableBalance().subtract(amount);
        BigDecimal newBlockedBalance = wallet.getBlockedBalance().add(amount);
        // total_balance reste inchangé car available + blocked = constant

        wallet.setAvailableBalance(newAvailableBalance);
        wallet.setBlockedBalance(newBlockedBalance);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);
    }

    /**
     * Débloque des fonds sur un wallet (les rend disponibles pour les transactions)
     */
    @Transactional
    public void unblockFunds(String walletNumber, BigDecimal amount) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet with number " + walletNumber));

        // Vérifier que les fonds bloqués sont suffisants
        if (wallet.getBlockedBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient blocked balance to unblock funds");
        }

        // Débloquer les fonds : available += amount, blocked -= amount, total reste inchangé
        BigDecimal newAvailableBalance = wallet.getAvailableBalance().add(amount);
        BigDecimal newBlockedBalance = wallet.getBlockedBalance().subtract(amount);
        // total_balance reste inchangé car available + blocked = constant

        wallet.setAvailableBalance(newAvailableBalance);
        wallet.setBlockedBalance(newBlockedBalance);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);
    }

    /**
     * Recalcule et met à jour total_balance pour maintenir la cohérence
     * total_balance = available_balance + blocked_balance
     */
    @Transactional
    public void recalculateTotalBalance(String walletNumber) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet with number " + walletNumber));

        BigDecimal availableBalance = wallet.getAvailableBalance() != null ? wallet.getAvailableBalance() : BigDecimal.ZERO;
        BigDecimal blockedBalance = wallet.getBlockedBalance() != null ? wallet.getBlockedBalance() : BigDecimal.ZERO;
        BigDecimal correctTotalBalance = availableBalance.add(blockedBalance);

        if (!correctTotalBalance.equals(wallet.getTotalBalance())) {
            wallet.setTotalBalance(correctTotalBalance);
            wallet.setUpdatedAt(LocalDateTime.now());
            walletRepository.save(wallet);
        }
    }

    /**
     * Vérifie la cohérence des soldes d'un wallet
     */
    public boolean isBalanceConsistent(String walletNumber) {
        try {
            Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet with number " + walletNumber));

            BigDecimal availableBalance = wallet.getAvailableBalance() != null ? wallet.getAvailableBalance() : BigDecimal.ZERO;
            BigDecimal blockedBalance = wallet.getBlockedBalance() != null ? wallet.getBlockedBalance() : BigDecimal.ZERO;
            BigDecimal totalBalance = wallet.getTotalBalance() != null ? wallet.getTotalBalance() : BigDecimal.ZERO;

            return totalBalance.equals(availableBalance.add(blockedBalance));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Vérifie et met à jour les limites quotidiennes.
     * Gele le wallet si la limite est dépassée.
     */
    private void checkAndUpdateDailyLimits(Wallet wallet, BigDecimal amount) {
        LocalDateTime now = LocalDateTime.now();

        // Reset daily spent si c'est un nouveau jour
        if (wallet.getLastLimitReset() == null ||
            !wallet.getLastLimitReset().toLocalDate().equals(now.toLocalDate())) {
            wallet.setDailySpent(BigDecimal.ZERO);
            wallet.setLastLimitReset(now);
        }

        // Calculer le nouveau daily spent
        BigDecimal newDailySpent = wallet.getDailySpent().add(amount);

        // Vérifier la limite quotidienne
        if (wallet.getDailyLimit() != null && newDailySpent.compareTo(wallet.getDailyLimit()) > 0) {
            // Geler le wallet
            wallet.setFrozenAt(now);
            wallet.setFrozenBy(0L); // System
            wallet.setFrozenReason("Daily limit exceeded");
            wallet.setStatus("FROZEN");
            walletRepository.save(wallet);
            throw new RuntimeException("Daily limit exceeded. Wallet frozen.");
        }

        // Mettre à jour daily spent
        wallet.setDailySpent(newDailySpent);

        // Reset monthly spent si c'est un nouveau mois
        if (wallet.getLastLimitReset() == null ||
            wallet.getLastLimitReset().getMonth() != now.getMonth()) {
            wallet.setMonthlySpent(BigDecimal.ZERO);
        }

        // Calculer et vérifier monthly spent
        BigDecimal newMonthlySpent = wallet.getMonthlySpent().add(amount);
        if (wallet.getMonthlyLimit() != null && newMonthlySpent.compareTo(wallet.getMonthlyLimit()) > 0) {
            // Geler le wallet
            wallet.setFrozenAt(now);
            wallet.setFrozenBy(0L); // System
            wallet.setFrozenReason("Monthly limit exceeded");
            wallet.setStatus("FROZEN");
            walletRepository.save(wallet);
            throw new RuntimeException("Monthly limit exceeded. Wallet frozen.");
        }

        wallet.setMonthlySpent(newMonthlySpent);
    }

    private WalletDTO toDTO(Wallet wallet) {
        return WalletDTO.builder()
                .id(wallet.getId())
                .walletNumber(wallet.getWalletNumber())
                .availableBalance(wallet.getAvailableBalance())
                .blockedBalance(wallet.getBlockedBalance())
                .totalBalance(wallet.getTotalBalance())
                .currency(wallet.getCurrency())
                .type(wallet.getType())
                .status(wallet.getStatus())
                .userId(wallet.getUserId())
                .dailyLimit(wallet.getDailyLimit())
                .dailySpent(wallet.getDailySpent())
                .monthlyLimit(wallet.getMonthlyLimit())
                .monthlySpent(wallet.getMonthlySpent())
                .frozenAt(wallet.getFrozenAt())
                .frozenBy(wallet.getFrozenBy())
                .frozenReason(wallet.getFrozenReason())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .closedAt(wallet.getClosedAt())
                .lastLimitReset(wallet.getLastLimitReset())
                .build();
    }
}

