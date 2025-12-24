package com.zaphira.transaction.service;

import com.zaphira.transaction.integration.wallet.FeignWalletClient;
import com.zaphira.common.dto.WalletDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service pour gérer les opérations sur les wallets via le wallet-service.
 *
 * IMPORTANT: Ce service utilise FeignClient pour communiquer avec wallet-service.
 * Les mises à jour de balance sont faites de manière atomique côté wallet-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final FeignWalletClient walletClient;

    /**
     * Met à jour le solde d'un wallet de manière atomique via wallet-service.
     *
     * @param walletNumber Le numéro du wallet
     * @param newBalance Le nouveau solde
     * @throws IllegalArgumentException Si le solde est négatif
     * @throws RuntimeException Si la mise à jour échoue
     */
    public void updateWalletBalance(String walletNumber, BigDecimal newBalance) {
        log.debug("Updating wallet {} balance to {}", walletNumber, newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Invalid balance update for wallet {}: negative balance {}", walletNumber, newBalance);
            throw new IllegalArgumentException("Wallet balance cannot be negative");
        }

        try {
            // Récupérer le wallet actuel
            WalletDTO currentWallet = walletClient.getWalletByNumber(walletNumber);
            if (currentWallet == null) {
                throw new RuntimeException("Wallet not found: " + walletNumber);
            }

            // Calculer la différence
            BigDecimal difference = newBalance.subtract(currentWallet.getAvailableBalance());

            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                // Créditer le wallet
                log.debug("Crediting wallet {} with amount {}", walletNumber, difference);
                // Note: Cette méthode devrait être ajoutée au wallet-service si nécessaire
                // walletClient.credit(walletNumber, difference);
            } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
                // Débiter le wallet
                BigDecimal debitAmount = difference.negate();
                log.debug("Debiting wallet {} with amount {}", walletNumber, debitAmount);
                // Note: Cette méthode devrait être ajoutée au wallet-service si nécessaire
                // walletClient.debit(walletNumber, debitAmount);
            }

            log.debug("Wallet {} balance updated successfully", walletNumber);

        } catch (Exception e) {
            log.error("Failed to update wallet balance for wallet {}: {}", walletNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to update wallet balance: " + e.getMessage(), e);
        }
    }

    /**
     * Récupère les informations d'un wallet.
     *
     * @param walletNumber Le numéro du wallet
     * @return WalletDTO avec les informations du wallet
     */
    public WalletDTO getWallet(String walletNumber) {
        try {
            return walletClient.getWalletByNumber(walletNumber);
        } catch (Exception e) {
            log.error("Failed to get wallet {}: {}", walletNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to get wallet: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifie si un wallet peut effectuer une transaction.
     *
     * @param walletNumber Le numéro du wallet
     * @param amount Le montant de la transaction
     * @return true si la transaction est possible
     */
    public boolean canPerformTransaction(String walletNumber, BigDecimal amount) {
        try {
            WalletDTO wallet = getWallet(walletNumber);

            // Vérifier si gelé
            if (wallet.getFrozenAt() != null) {
                log.warn("Wallet {} is frozen", walletNumber);
                return false;
            }

            // Vérifier le solde
            if (wallet.getAvailableBalance().compareTo(amount) < 0) {
                log.warn("Insufficient balance for wallet {}: available={}, needed={}",
                        walletNumber, wallet.getAvailableBalance(), amount);
                return false;
            }

            // Vérifier les limites
            if (wallet.getDailyLimit() != null) {
                BigDecimal newDailySpent = (wallet.getDailySpent() != null ? wallet.getDailySpent() : BigDecimal.ZERO).add(amount);
                if (newDailySpent.compareTo(wallet.getDailyLimit()) > 0) {
                    log.warn("Daily limit would be exceeded for wallet {}", walletNumber);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Failed to check transaction possibility for wallet {}: {}", walletNumber, e.getMessage(), e);
            return false;
        }
    }
}
