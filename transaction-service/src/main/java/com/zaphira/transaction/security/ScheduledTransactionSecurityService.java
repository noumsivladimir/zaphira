package com.zaphira.transaction.security;

import com.zaphira.transaction.model.ScheduledTransaction;
import com.zaphira.transaction.repository.ScheduledTransactionRepository;
import com.zaphira.transaction.service.ScheduledTransactionNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * ScheduledTransactionSecurityService - Custom security for scheduled transactions.
 * 
 * Used in @PreAuthorize expressions:
 * - @scheduledTxSecurity.isOwner(id)
 */
@Service("scheduledTxSecurity")
@RequiredArgsConstructor
@Slf4j
public class ScheduledTransactionSecurityService {

    private final ScheduledTransactionRepository scheduledTransactionRepository;

    /**
     * Check if current user is the owner (requester) of the scheduled transaction.
     * 
     * @param scheduledTransactionId ID of the scheduled transaction
     * @return true if current user is the requester, false otherwise
     */
    public boolean isOwner(Long scheduledTransactionId) {
        try {
            // Get current user ID from security context
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                log.warn("[SCHEDULED_TX_SECURITY] No user ID in security context");
                return false;
            }

            // Find scheduled transaction
            ScheduledTransaction scheduled = scheduledTransactionRepository.findById(scheduledTransactionId)
                .orElseThrow(() -> new ScheduledTransactionNotFoundException(scheduledTransactionId));

            // Check ownership
            boolean isOwner = scheduled.getRequesterUserId() != null 
                && scheduled.getRequesterUserId().equals(currentUserId);

            log.debug("[SCHEDULED_TX_SECURITY] User {} isOwner of scheduled tx {}: {}", 
                currentUserId, scheduledTransactionId, isOwner);

            return isOwner;

        } catch (ScheduledTransactionNotFoundException e) {
            log.warn("[SCHEDULED_TX_SECURITY] Scheduled transaction not found: {}", scheduledTransactionId);
            return false;
        } catch (Exception e) {
            log.error("[SCHEDULED_TX_SECURITY] Error checking ownership for scheduled tx {}: {}", 
                scheduledTransactionId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract current user ID from security context.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        try {
            // Extract userId from JWT claims (added by JwtAuthenticationFilter)
            Object principal = authentication.getPrincipal();
            if (principal instanceof AuthenticatedUser) {
                return ((AuthenticatedUser) principal).getId();
            }

            // Fallback: extract from authentication details/attributes
            Object userIdAttr = authentication.getDetails();
            if (userIdAttr instanceof Long) {
                return (Long) userIdAttr;
            }

            log.warn("[SCHEDULED_TX_SECURITY] Unable to extract userId from authentication");
            return null;

        } catch (Exception e) {
            log.error("[SCHEDULED_TX_SECURITY] Error extracting userId: {}", e.getMessage());
            return null;
        }
    }
}
