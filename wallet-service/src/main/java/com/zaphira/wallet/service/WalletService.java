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

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    //private final TransactionServiceClient transactionServiceClient;
   public WalletDTO createWallet(Long userId) {
    // Générer walletNumber unique à 8 chiffres basé sur l'ID utilisateur
    String walletNumber = String.format("%08d", (userId * 1234567) % 100_000_000);

    // Créer le wallet avec tous les champs initialisés correctement
    Wallet wallet = Wallet.builder()
            .userId(userId)
            .walletNumber(walletNumber)
            // Balance fields initialized to 0
            .availableBalance(BigDecimal.ZERO)
            .blockedBalance(BigDecimal.ZERO)
            .totalBalance(BigDecimal.ZERO)
            // Default values
            .status("ACTIVE")
            .isPrimary(false)
            .type("REGULAR")
            .currency("XOF")
            // Limits initialized to null/default
            .dailyLimit(null)
            .dailySpent(BigDecimal.ZERO)
            .monthlyLimit(null)
            .monthlySpent(BigDecimal.ZERO)
            // Frozen status
            .frozenAt(null)
            .frozenBy(null)
            .frozenReason(null)
            // Other fields
            .metadata(null)
            .version(0L)
            .createdAt(java.time.LocalDateTime.now())
            .updatedAt(java.time.LocalDateTime.now())
            .closedAt(null)
            .lastLimitReset(null)
            .build();

    // Sauvegarder le wallet en base
    Wallet saved = walletRepository.save(wallet);

    return toDTO(saved);
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
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    @Transactional
    public void credit(String walletNumber, BigDecimal amount) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet with number " + walletNumber));
        
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    @Transactional
    public void transfer(String senderWalletNumber, String receiverWalletNumber, BigDecimal amount) {
        debit(senderWalletNumber, amount);
        credit(receiverWalletNumber, amount);
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
                .isPrimary(wallet.getIsPrimary())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .closedAt(wallet.getClosedAt())
                .lastLimitReset(wallet.getLastLimitReset())
                .metadata(wallet.getMetadata())
                .version(wallet.getVersion())
                .build();
    }
}

