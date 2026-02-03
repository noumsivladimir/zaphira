package com.zaphira.wallet.security;

import com.zaphira.common.model.enums.RoleType;
import com.zaphira.common.security.SecurityContextHolder;

import com.zaphira.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Custom security expression handler for wallet operations
 * Used with @PreAuthorize("@walletSecurity.isOwner(#walletNumber)")
 */
@Component("walletSecurity")
@RequiredArgsConstructor
@Slf4j
public class WalletSecurityExpression {
    
    private final WalletRepository walletRepository;
    
    /**
     * Check if current user is the owner of the wallet
     * 
     * @param walletNumber Wallet number
     * @return true if user is the owner
     */
    public boolean isOwner(String walletNumber) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            log.warn("No authenticated user found");
            return false;
        }
        
        return walletRepository.findByWalletNumber(walletNumber)
                .map(wallet -> currentUserId.equals(wallet.getUserId()))
                .orElse(false);
    }
    
    /**
     * Check if user can view this wallet
     * - ADMIN can view all
     * - REGULAR/MERCHANT can view their own wallets
     * 
     * @param walletNumber Wallet number
     * @return true if user can view
     */
    public boolean canView(String walletNumber) {
        // Admins can view all wallets
        if (SecurityContextHolder.hasRole(RoleType.ADMIN)) {
            return true;
        }
        
        // Users can view their own wallets
        return isOwner(walletNumber);
    }
    
    /**
     * Check if user can perform transactions from this wallet
     * 
     * @param walletNumber Wallet number
     * @return true if user can transact
     */
    public boolean canTransact(String walletNumber) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        
        return walletRepository.findByWalletNumber(walletNumber)
                .map(wallet -> {
                    // Must be owner
                    if (!currentUserId.equals(wallet.getUserId())) {
                        return false;
                    }
                    
                    // Wallet must be active
                    return wallet.isActive();
                })
                .orElse(false);
    }
    
    /**
     * Check if merchant can access their merchant wallet features
     * 
     * @param walletNumber Wallet number
     * @return true if merchant owns wallet
     */
    public boolean isMerchantWallet(String walletNumber) {
        if (!SecurityContextHolder.isMerchant()) {
            return false;
        }
        
        Long currentMerchantId = SecurityContextHolder.getCurrentMerchantId();
        
        return walletRepository.findByWalletNumber(walletNumber)
                .map(wallet -> currentMerchantId.equals(wallet.getUserId()))
                .orElse(false);
    }
    
    /**
     * Check if user can freeze/unfreeze wallet
     * Only ADMIN can freeze wallets
     * 
     * @param walletNumber Wallet number
     * @return true if user can freeze
     */
    public boolean canFreeze(String walletNumber) {
        return SecurityContextHolder.hasRole(RoleType.ADMIN);
    }
    
    /**
     * Check if user can view wallet balance
     * 
     * @param walletNumber Wallet number
     * @return true if user can view balance
     */
    public boolean canViewBalance(String walletNumber) {
        return canView(walletNumber);
    }
    
    /**
     * Check if user can view wallet transaction history
     * 
     * @param walletNumber Wallet number
     * @return true if user can view history
     */
    public boolean canViewHistory(String walletNumber) {
        return canView(walletNumber);
    }
}
