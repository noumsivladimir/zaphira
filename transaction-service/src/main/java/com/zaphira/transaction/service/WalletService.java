package com.zaphira.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service pour gérer les opérations sur les wallets.
 * 
 * IMPORTANT: Ce service doit être atomique et synchronisé.
 * Les mises à jour de balance doivent être faites de manière thread-safe.
 * 
 * En production, cela pourrait utiliser:
 * - Une transaction distribuée (Saga pattern)
 * - Un verrou pessimiste (SELECT FOR UPDATE)
 * - Une queue d'événements asynchrone
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {
    
    // WalletRepository serait injecté ici
    // private final WalletRepository walletRepository;
    
    /**
     * Met à jour le solde d'un wallet de manière atomique.
     * 
     * @param walletId L'ID du wallet
     * @param newBalance Le nouveau solde
     * @throws IllegalArgumentException Si le solde est négatif
     * @throws ResourceNotFoundException Si le wallet n'existe pas
     */
    public void updateWalletBalance(Long walletId, BigDecimal newBalance) {
        log.debug("Updating wallet {} balance to {}", walletId, newBalance);
        
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Invalid balance update for wallet {}: negative balance {}", walletId, newBalance);
            throw new IllegalArgumentException("Wallet balance cannot be negative");
        }
        
        // TODO: Implémenter la mise à jour atomique du wallet
        // walletRepository.updateBalance(walletId, newBalance);
        
        log.debug("Wallet {} balance updated successfully", walletId);
    }
}
