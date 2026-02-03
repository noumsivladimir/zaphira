package com.zaphira.transaction.security;

import com.zaphira.common.model.enums.RoleType;
import com.zaphira.common.security.SecurityContextHolder;
import com.zaphira.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Custom security expression handler for transaction operations
 * Used with @PreAuthorize("@txSecurity.isOwner(#transactionRef)")
 */
@Component("txSecurity")
@RequiredArgsConstructor
@Slf4j
public class TransactionSecurityExpression {
    
    private final TransactionRepository transactionRepository;
    
    /**
     * Check if current user is the owner (sender) of the transaction
     * 
     * @param transactionRef Transaction reference
     * @return true if user is the sender
     */
    public boolean isOwner(String transactionRef) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            log.warn("No authenticated user found");
            return false;
        }
        
        return transactionRepository.findByReference(transactionRef)
                .map(tx -> currentUserId.equals(tx.getSenderWalletId()))
                .orElse(false);
    }
    
    /**
     * Check if current user is a participant (sender OR receiver) of the transaction
     * 
     * @param transactionRef Transaction reference
     * @return true if user is sender or receiver
     */
    public boolean isParticipant(String transactionRef) {
        Long currentUserId = SecurityContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            log.warn("No authenticated user found");
            return false;
        }
        
        return transactionRepository.findByReference(transactionRef)
                .map(tx -> currentUserId.equals(tx.getSenderWalletId()) || currentUserId.equals(tx.getReceiverWalletId()))
                .orElse(false);
    }
    
    /**
     * Check if current merchant user owns this transaction
     * (transaction was sent by a user TO this merchant)
     * 
     * @param transactionRef Transaction reference
     * @return true if current merchant is the receiver
     */
    public boolean isMerchantOwner(String transactionRef) {
        if (!SecurityContextHolder.isMerchant()) {
            log.warn("Current user is not a merchant");
            return false;
        }
        
        Long currentMerchantId = SecurityContextHolder.getCurrentMerchantId();
        
        return transactionRepository.findByReference(transactionRef)
                .map(tx -> currentMerchantId.equals(tx.getReceiverWalletId()))
                .orElse(false);
    }
    
    /**
     * Check if user can view this transaction
     * - ADMIN can view all
     * - REGULAR/MERCHANT can view if they are participant
     * 
     * @param transactionRef Transaction reference
     * @return true if user can view
     */
    public boolean canView(String transactionRef) {
        // Admins can view all transactions
        if (SecurityContextHolder.hasRole(RoleType.ADMIN)) {
            return true;
        }
        
        // Regular users and merchants can view if they are participants
        return isParticipant(transactionRef);
    }
    
    /**
     * Check if user can refund this transaction
     * - ADMIN can refund any
     * - MERCHANT can refund their own received payments
     * 
     * @param transactionRef Transaction reference
     * @return true if user can refund
     */
    public boolean canRefund(String transactionRef) {
        // Admins can refund any transaction
        if (SecurityContextHolder.hasRole(RoleType.ADMIN)) {
            return true;
        }
        
        // Merchants can refund transactions they received
        if (SecurityContextHolder.isMerchant()) {
            return isMerchantOwner(transactionRef);
        }
        
        return false;
    }
    
    /**
     * Check if user can initiate dispute
     * - Sender can dispute
     * 
     * @param transactionRef Transaction reference
     * @return true if user can dispute
     */
    public boolean canDispute(String transactionRef) {
        return isOwner(transactionRef);
    }
    
    /**
     * Check if merchant can view their settlements
     * 
     * @param merchantId Merchant user ID
     * @return true if current merchant matches
     */
    public boolean isMerchantSettlement(Long merchantId) {
        if (!SecurityContextHolder.isMerchant()) {
            return false;
        }
        
        Long currentMerchantId = SecurityContextHolder.getCurrentMerchantId();
        return currentMerchantId.equals(merchantId);
    }
}
