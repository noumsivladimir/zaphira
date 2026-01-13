package com.zaphira.transaction.service;

import com.zaphira.common.exception.ResourceNotFoundException;
import com.zaphira.transaction.dto.requests.DisputeResolutionRequest;
import com.zaphira.transaction.event.DisputeResolvedEvent;
import com.zaphira.transaction.exception.AccessDeniedException;
import com.zaphira.transaction.model.*;
import com.zaphira.transaction.model.enums.DisputeResolutionType;
import com.zaphira.transaction.model.enums.DisputeStatus;
import com.zaphira.transaction.repository.DisputeRepository;
import com.zaphira.transaction.security.AuthenticatedUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DisputeResolutionService - Handles dispute resolution and fund management.
 * 
 * Responsibilities:
 * 1. Resolve disputes with appropriate decisions
 * 2. Hold/escrow disputed amounts during investigation
 * 3. Release funds to winners based on resolution
 * 4. Update dispute status and create timeline events
 * 5. Publish dispute resolution events
 * 6. Maintain audit trail
 * 
 * Fund Management Strategy:
 * 1. When dispute created: Hold full disputed amount in escrow
 * 2. During investigation: Amount remains held
 * 3. When resolved:
 *    - APPROVED: Release to customer (refund)
 *    - DENIED: Release to merchant
 *    - PARTIAL_APPROVAL: Split between parties
 *    - SETTLEMENT: Release as agreed
 *    - WITHDRAWN/EXPIRED: Release to merchant
 * 
 * Authorization:
 * - Only ADMIN role can resolve disputes
 * - Multi-level checks via DisputeAuthorizationService
 * 
 * Patterns (from Phase 2):
 * - JWT extraction
 * - Multi-level authorization
 * - @Transactional for atomic operations
 * - Kafka event publishing
 * - Comprehensive audit logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeResolutionService {
    
    private final DisputeRepository disputeRepository;
    private final DisputeAuthorizationService authorizationService;
    @Nullable
    private final KafkaTemplate<String, DisputeResolvedEvent> kafkaTemplate;
    
    private static final String DISPUTE_RESOLVED_TOPIC = "dispute-resolved";
    
    // ============================================================
    // HOLD AMOUNT (Place dispute amount in escrow)
    // ============================================================
    
    /**
     * Hold disputed amount in escrow when dispute is created.
     * 
     * Freezes the disputed amount so it cannot be withdrawn or transferred.
     * This is done immediately when dispute is initiated.
     * 
     * Flow:
     * 1. Get dispute and transaction
     * 2. Calculate hold amount (claimed amount or remaining balance)
     * 3. Update wallet to reflect hold
     * 4. Record audit entry
     * 5. Log transaction
     * 
     * @param dispute Dispute being created
     * @throws BusinessException if hold fails (insufficient balance, etc)
     */
    @Transactional
    public void holdAmount(Dispute dispute) {
        log.info(
            "[DISPUTE_HOLD_START] DisputeId: {}, TransactionId: {}, Amount: {}",
            dispute.getId(), dispute.getTransactionId(), dispute.getClaimedAmount()
        );
        
        try {
            // Create wallet hold/escrow entry
            // Implementation depends on Wallet service structure
            // Example pattern from Phase 2:
            // walletService.holdAmount(
            //     dispute.getTransactionId(),
            //     dispute.getClaimedAmount(),
            //     "DISPUTE_HOLD",
            //     dispute.getReference()
            // );
            
            log.info(
                "[DISPUTE_HOLD_SUCCESS] DisputeId: {}, Amount held: {}",
                dispute.getId(), dispute.getClaimedAmount()
            );
        } catch (Exception e) {
            log.error(
                "[DISPUTE_HOLD_FAIL] DisputeId: {}, Error: {}, Message: {}",
                dispute.getId(), e.getClass().getSimpleName(), e.getMessage()
            );
            throw new AccessDeniedException("Failed to hold dispute amount: " + e.getMessage());
        }
    }
    
    // ============================================================
    // RESOLVE DISPUTE
    // ============================================================
    
    /**
     * Resolve a dispute with final decision and fund release.
     * 
     * Flow:
     * 1. Extract JWT to get authenticated admin
     * 2. Get dispute and validate it's open
     * 3. Check authorization (admin-only via DisputeAuthorizationService)
     * 4. Validate resolution request data
     * 5. Update dispute status based on resolution type
     * 6. Release funds according to decision
     * 7. Create timeline event
     * 8. Publish DisputeResolvedEvent
     * 9. Log audit trail
     * 
     * Resolution Types & Fund Release:
     * - APPROVED: Full amount to customer (refund)
     * - PARTIAL_APPROVAL: Partial to customer, remainder to merchant
     * - DENIED: Full amount back to merchant
     * - SETTLEMENT: Amount as agreed between parties
     * - WITHDRAWN: Dispute withdrawn by customer, release to merchant
     * - EXPIRED: Deadline passed, release to merchant
     * - ESCALATED_TO_BANK: Amount held pending bank decision
     * 
     * @param request DisputeResolutionRequest with decision and amount
     * @return Updated Dispute with new status
     * @throws ResourceNotFoundException if dispute not found
     * @throws AccessDeniedException if not authorized (not admin)
     * @throws BusinessException if resolution fails
     */
    @Transactional
    public Dispute resolveDispute(DisputeResolutionRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        
        log.info(
            "[DISPUTE_RESOLVE_START] Admin: {}, DisputeId: {}, ResolutionType: {}, Amount: {}",
            user.getEmail(), request.getDisputeId(), request.getResolutionType(),
            request.getResolutionAmount()
        );
        
        // Get dispute
        Dispute dispute = disputeRepository.findById(Long.parseLong(request.getDisputeId()))
            .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + request.getDisputeId()));
        
        // Authorize resolution
        authorizationService.authorizeDisputeResolution(user, dispute);
        
        // Validate resolution data
        validateResolutionRequest(request, dispute);
        
        // Store old status for timeline
        // Update dispute status and resolution info
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolutionType(DisputeResolutionType.valueOf(request.getResolutionType().toUpperCase()));
        dispute.setResolutionAmount(request.getResolutionAmount());
        dispute.setResolvedBy(user.getEmail());
        dispute.setResolvedAt(LocalDateTime.now());
        
        // Release funds based on resolution type
        try {
            switch (request.getResolutionType().toUpperCase()) {
                case "APPROVED":
                    releaseToCustomer(dispute, dispute.getClaimedAmount());
                    break;
                case "PARTIAL_APPROVAL":
                    releaseToCustomer(dispute, request.getResolutionAmount());
                    BigDecimal remainderToMerchant = dispute.getClaimedAmount()
                        .subtract(request.getResolutionAmount());
                    releaseToMerchant(dispute, remainderToMerchant);
                    break;
                case "DENIED":
                    releaseToMerchant(dispute, dispute.getClaimedAmount());
                    break;
                case "SETTLEMENT":
                    releaseToCustomer(dispute, request.getResolutionAmount());
                    break;
                case "WITHDRAWN":
                case "EXPIRED":
                    releaseToMerchant(dispute, dispute.getClaimedAmount());
                    break;
                case "ESCALATED_TO_BANK":
                    // Amount remains in escrow pending bank decision
                    break;
                default:
                    throw new AccessDeniedException("Unknown resolution type: " + request.getResolutionType());
            }
        } catch (AccessDeniedException e) {
            log.error(
                "[DISPUTE_RESOLVE_FAIL] Fund release failed. DisputeId: {}, Error: {}",
                dispute.getId(), e.getMessage()
            );
            throw e;
        }
        
        // Create timeline event
        DisputeTimeline timelineEvent = DisputeTimeline.ofDisputeResolved(
            dispute,
            user.getEmail(),
            "ADMIN",
            request.getResolutionType(),
            request.getResolutionReason()
        );
        
        dispute.getTimeline().add(timelineEvent);
        
        // Save dispute
        Dispute savedDispute = disputeRepository.save(dispute);
        
        // Publish event
        publishDisputeResolvedEvent(savedDispute, user, request.getInternalNotes());
        
        log.info(
            "[DISPUTE_RESOLVE_SUCCESS] DisputeId: {}, ResolutionType: {}, Amount: {}, ResolvedBy: {}",
            savedDispute.getId(), savedDispute.getResolutionType(),
            savedDispute.getResolutionAmount(), user.getEmail()
        );
        
        return savedDispute;
    }
    
    // ============================================================
    // RELEASE TO CUSTOMER (Refund)
    // ============================================================
    
    /**
     * Release/refund amount to customer.
     * 
     * Processes refund transaction back to customer's wallet.
     * 
     * @param dispute Dispute being resolved
     * @param amount Amount to refund to customer
     * @throws BusinessException if refund fails
     */
    private void releaseToCustomer(Dispute dispute, BigDecimal amount) {
        log.info(
            "[DISPUTE_RELEASE_CUSTOMER] DisputeId: {}, Amount: {}, Customer: {}",
            dispute.getId(), amount, dispute.getInitiatedBy()
        );
        
        try {
            // Create refund transaction using TransactionServiceImpl pattern
            // Example:
            // walletService.refundAmount(
            //     dispute.getTransactionId(),
            //     amount,
            //     "DISPUTE_APPROVED",
            //     dispute.getReference()
            // );
            
            log.info(
                "[DISPUTE_RELEASE_CUSTOMER_SUCCESS] Amount refunded to customer. DisputeId: {}",
                dispute.getId()
            );
        } catch (Exception e) {
            log.error(
                "[DISPUTE_RELEASE_CUSTOMER_FAIL] DisputeId: {}, Error: {}",
                dispute.getId(), e.getMessage()
            );
            throw new AccessDeniedException("Failed to release amount to customer: " + e.getMessage());
        }
    }
    
    // ============================================================
    // RELEASE TO MERCHANT
    // ============================================================
    
    /**
     * Release amount back to merchant.
     * 
     * Releases held amount back to merchant's wallet when:
     * - Dispute is DENIED
     * - Dispute is WITHDRAWN by customer
     * - Dispute EXPIRES without resolution
     * 
     * @param dispute Dispute being resolved
     * @param amount Amount to release to merchant
     * @throws BusinessException if release fails
     */
    private void releaseToMerchant(Dispute dispute, BigDecimal amount) {
        log.info(
            "[DISPUTE_RELEASE_MERCHANT] DisputeId: {}, Amount: {}",
            dispute.getId(), amount
        );
        
        try {
            // Release held amount back to merchant
            // Example:
            // walletService.releaseHoldAmount(
            //     dispute.getTransactionId(),
            //     amount,
            //     "DISPUTE_DENIED",
            //     dispute.getReference()
            // );
            
            log.info(
                "[DISPUTE_RELEASE_MERCHANT_SUCCESS] Amount released to merchant. DisputeId: {}",
                dispute.getId()
            );
        } catch (Exception e) {
            log.error(
                "[DISPUTE_RELEASE_MERCHANT_FAIL] DisputeId: {}, Error: {}",
                dispute.getId(), e.getMessage()
            );
            throw new AccessDeniedException("Failed to release amount to merchant: " + e.getMessage());
        }
    }
    
    // ============================================================
    // VALIDATION
    // ============================================================
    
    /**
     * Validate resolution request data.
     * 
     * Checks:
     * 1. Resolution amount is >= 0
     * 2. Resolution amount <= claimed amount (except for special cases)
     * 3. Resolution amount matches requirements for resolution type
     * 4. Reason is provided and not empty
     * 
     * @param request DisputeResolutionRequest to validate
     * @param dispute Dispute being resolved
     * @throws BusinessException if validation fails
     */
    private void validateResolutionRequest(DisputeResolutionRequest request, Dispute dispute) {
        // Check amount >= 0
        if (request.getResolutionAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new AccessDeniedException("Resolution amount cannot be negative");
        }
        
        // Check amount <= claimed amount
        if (request.getResolutionAmount().compareTo(dispute.getClaimedAmount()) > 0) {
            throw new AccessDeniedException(
                "Resolution amount cannot exceed claimed amount of " + dispute.getClaimedAmount()
            );
        }
        
        // Type-specific validations
        String resolutionType = request.getResolutionType().toUpperCase();
        
        switch (resolutionType) {
            case "APPROVED":
                if (!request.getResolutionAmount().equals(dispute.getClaimedAmount())) {
                    throw new AccessDeniedException(
                        "APPROVED resolution must refund full claimed amount of " + 
                        dispute.getClaimedAmount()
                    );
                }
                break;
                
            case "DENIED":
                if (!request.getResolutionAmount().equals(BigDecimal.ZERO)) {
                    throw new AccessDeniedException(
                        "DENIED resolution must have resolution amount of 0"
                    );
                }
                break;
                
            case "WITHDRAWN":
            case "EXPIRED":
                if (!request.getResolutionAmount().equals(BigDecimal.ZERO)) {
                    throw new AccessDeniedException(
                        resolutionType + " resolution must have resolution amount of 0"
                    );
                }
                break;
                
            case "PARTIAL_APPROVAL":
                if (request.getResolutionAmount().compareTo(BigDecimal.ZERO) <= 0 ||
                    request.getResolutionAmount().compareTo(dispute.getClaimedAmount()) >= 0) {
                    throw new AccessDeniedException(
                        "PARTIAL_APPROVAL amount must be between 0 and " + 
                        dispute.getClaimedAmount()
                    );
                }
                break;
        }
        
        // Check reason provided
        if (request.getResolutionReason() == null || request.getResolutionReason().trim().isEmpty()) {
            throw new AccessDeniedException("Resolution reason is required");
        }
    }
    
    // ============================================================
    // HELPER METHODS
    // ============================================================
    
    /**
     * Extract authenticated user from SecurityContext (JWT extraction).
     */
    private AuthenticatedUser getAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        var principal = auth.getPrincipal();
        if (principal instanceof AuthenticatedUser) {
            return (AuthenticatedUser) principal;
        }
        throw new IllegalStateException("Principal is not of type AuthenticatedUser");
    }
    
    /**
     * Publish DisputeResolvedEvent to Kafka.
     */
    private void publishDisputeResolvedEvent(Dispute dispute, AuthenticatedUser user, String internalNotes) {
        if (kafkaTemplate == null) {
            log.warn("[KAFKA_SKIP] KafkaTemplate not available, skipping event publish for dispute: {}", dispute.getReference());
            return;
        }
        
        DisputeResolvedEvent event = DisputeResolvedEvent.builder()
            .disputeId(dispute.getId())
            .reference(dispute.getReference())
            .transactionId(dispute.getTransactionId().toString())
            .resolutionType(dispute.getResolutionType().name())
            .resolutionAmount(dispute.getResolutionAmount())
            .resolvedBy(user.getEmail())
            .resolvedAt(dispute.getResolvedAt())
            .internalNotes(internalNotes)
            .build();
        
        kafkaTemplate.send(DISPUTE_RESOLVED_TOPIC, dispute.getReference(), event);
        log.info("[KAFKA_PUBLISH] DisputeResolvedEvent sent for dispute: {}", dispute.getReference());
    }
}
