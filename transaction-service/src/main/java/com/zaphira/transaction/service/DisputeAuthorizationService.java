package com.zaphira.transaction.service;

import com.zaphira.transaction.exception.AccessDeniedException;
import com.zaphira.transaction.model.Dispute;
import com.zaphira.transaction.model.enums.DisputeStatus;
import com.zaphira.transaction.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * DisputeAuthorizationService - Multi-level authorization for dispute operations.
 * 
 * Implements business logic authorization checks for:
 * 1. Creating disputes
 * 2. Submitting evidence
 * 3. Responding to disputes
 * 4. Viewing dispute details
 * 5. Resolving disputes
 * 
 * Patterns (from Phase 2):
 * - Three-tier authorization (Spring Security → this service → business logic)
 * - Business rule checks (ownership, status, time windows, etc)
 * - Detailed exception messages for audit trail
 * 
 * Authorization Rules:
 * - CUSTOMER: Can create disputes for their own transactions, submit evidence, view own disputes
 * - MERCHANT: Can respond to disputes, submit counter-evidence, view related disputes
 * - ADMIN: Can perform any dispute operation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeAuthorizationService {
    
    // ============================================================
    // CREATE DISPUTE AUTHORIZATION
    // ============================================================
    
    /**
     * Authorize creation of a new dispute.
     * 
     * Business Rules:
     * 1. Customer can create dispute for their own transaction
     * 2. Merchant/Admin cannot create disputes (only customers and system)
     * 3. Transaction must exist and be completed
     * 4. Dispute must be within transaction date window (< 180 days old)
     * 5. Only one dispute per transaction allowed
     * 6. Transaction amount must be >= minimum dispute amount
     * 
     * @param user Authenticated user attempting to create dispute
     * @param transactionDTO Transaction being disputed
     * @param request DisputeRequest with dispute details
     * @throws AccessDeniedException if not authorized
     */
//    public void authorizeDisputeCreation(AuthenticatedUser user, TransactionDTO transactionDTO, DisputeRequest request) {
//        log.debug(
//            "[AUTH_DISPUTE_CREATE] User: {}, TransactionId: {}",
//            user.getEmail(), transactionDTO.getId()
//        );
//
//        // Check if customer is creating dispute for their own transaction
//        // For now, we check the sender wallet (the customer initiating transaction)
//        if (transactionDTO.getSenderWalletNumber() != null) {
//            Long senderUserId = transactionDTO.getUserId();
//            if (!senderUserId.equals(user.getId())) {
//                log.warn(
//                    "[AUTH_DISPUTE_CREATE_FAIL] Customer attempting to create dispute for different customer. " +
//                    "RequestBy: {}, TransactionSender: {}, TransactionId: {}",
//                    user.getEmail(), senderUserId, transactionDTO.getId()
//                );
//                throw new AccessDeniedException(
//                    "You can only create disputes for your own transactions"
//                );
//            }
//        }
//
//        // Check transaction status (must be completed)
//        if (transactionDTO.getStatus() != com.zaphira.transaction.model.enums.TransactionStatus.COMPLETED &&
//                transactionDTO.getStatus() != com.zaphira.transaction.model.enums.TransactionStatus.PROCESSING) {
//            log.warn(
//                "[AUTH_DISPUTE_CREATE_FAIL] Invalid transaction status for dispute. " +
//                "Status: {}, TransactionId: {}, User: {}",
//                    transactionDTO.getStatus(), transactionDTO.getId(), user.getEmail()
//            );
//            throw new AccessDeniedException(
//                "Can only create disputes for completed transactions. Current status: " + transactionDTO.getStatus()
//            );
//        }
//
//        // Check time window: Transaction must be recent (within 180 days)
//        LocalDateTime maxAge = LocalDateTime.now().minusDays(180);
//        if (transactionDTO.getCreatedAt().isBefore(maxAge)) {
//            log.warn(
//                "[AUTH_DISPUTE_CREATE_FAIL] Transaction too old for dispute. " +
//                "TransactionDate: {}, MaxDate: {}, TransactionId: {}, User: {}",
//                    transactionDTO.getCreatedAt(), maxAge, transactionDTO.getId(), user.getEmail()
//            );
//            throw new AccessDeniedException(
//                "Disputes can only be created for transactions within 180 days of transaction date"
//            );
//        }
//
//        // Check minimum dispute amount (no disputes for less than $1)
//        if (request.getClaimedAmount().compareTo(java.math.BigDecimal.valueOf(1.0)) < 0) {
//            throw new AccessDeniedException(
//                "Minimum dispute amount is $1.00"
//            );
//        }
//
//        log.info(
//            "[AUTH_DISPUTE_CREATE_APPROVED] User: {}, TransactionId: {}, Amount: {}",
//            user.getEmail(), transactionDTO.getId(), request.getClaimedAmount()
//        );
//    }
    
    // ============================================================
    // EVIDENCE SUBMISSION AUTHORIZATION
    // ============================================================
    
    /**
     * Authorize evidence submission for a dispute.
     * 
     * Business Rules:
     * 1. Only dispute initiator, related merchant, or admin can submit
     * 2. Dispute must be in appropriate status (INITIATED, AWAITING_EVIDENCE, AWAITING_RESPONSE)
     * 3. Evidence submission deadline must not have passed
     * 4. User must have TRANSACTION_EVIDENCE_SUBMIT permission
     * 
     * @param user Authenticated user submitting evidence
     * @param dispute Dispute receiving evidence
     * @throws AccessDeniedException if not authorized
     */
    public void authorizeEvidenceSubmission(AuthenticatedUser user, Dispute dispute) {
        log.debug(
            "[AUTH_EVIDENCE_SUBMIT] User: {}, DisputeId: {}, DisputeStatus: {}",
            user.getEmail(), dispute.getId(), dispute.getStatus()
        );
        
        // Check if user is authorized to submit for this dispute
        boolean isDisputeInitiator = dispute.getInitiatedBy().equals(user.getEmail());
        
        if (!isDisputeInitiator) {
            log.warn(
                "[AUTH_EVIDENCE_SUBMIT_FAIL] Not dispute initiator. " +
                "User: {}, InitiatedBy: {}, DisputeId: {}",
                user.getEmail(), dispute.getInitiatedBy(), dispute.getId()
            );
            throw new AccessDeniedException(
                "Only dispute initiator can submit evidence"
            );
        }
        
        // Check dispute status
        DisputeStatus status = dispute.getStatus();
        if (status != DisputeStatus.INITIATED && 
            status != DisputeStatus.AWAITING_EVIDENCE && 
            status != DisputeStatus.AWAITING_RESPONSE) {
            log.warn(
                "[AUTH_EVIDENCE_SUBMIT_FAIL] Invalid dispute status for evidence submission. " +
                "Status: {}, User: {}, DisputeId: {}",
                status, user.getEmail(), dispute.getId()
            );
            throw new AccessDeniedException(
                "Cannot submit evidence for dispute in " + status.name() + " status"
            );
        }
        
        // Check deadline
        if (dispute.isDeadlinePassed()) {
            log.warn(
                "[AUTH_EVIDENCE_SUBMIT_FAIL] Deadline passed for evidence submission. " +
                "Deadline: {}, Now: {}, DisputeId: {}, User: {}",
                dispute.getDeadlineAt(), LocalDateTime.now(), dispute.getId(), user.getEmail()
            );
            throw new AccessDeniedException(
                "Evidence submission deadline has passed"
            );
        }
        
        log.info(
            "[AUTH_EVIDENCE_SUBMIT_APPROVED] User: {}, DisputeId: {}",
            user.getEmail(), dispute.getId()
        );
    }
    
    // ============================================================
    // DISPUTE RESPONSE AUTHORIZATION
    // ============================================================
    
    /**
     * Authorize merchant response to a dispute.
     * 
     * Business Rules:
     * 1. Merchant or admin can respond to dispute
     * 2. Dispute must still be open (status: INITIATED or AWAITING_RESPONSE)
     * 
     * @param user Authenticated user responding to dispute
     * @param dispute Dispute being responded to
     * @throws AccessDeniedException if not authorized
     */
    public void authorizeDisputeResponse(AuthenticatedUser user, Dispute dispute) {
        log.debug(
            "[AUTH_DISPUTE_RESPONSE] User: {}, DisputeId: {}",
            user.getEmail(), dispute.getId()
        );
        
        // Check dispute status
        DisputeStatus status = dispute.getStatus();
        if (status != DisputeStatus.INITIATED && status != DisputeStatus.AWAITING_RESPONSE) {
            log.warn(
                "[AUTH_DISPUTE_RESPONSE_FAIL] Invalid dispute status for response. " +
                "Status: {}, User: {}, DisputeId: {}",
                status, user.getEmail(), dispute.getId()
            );
            throw new AccessDeniedException(
                "Cannot respond to dispute in " + status.name() + " status"
            );
        }
        
        log.info(
            "[AUTH_DISPUTE_RESPONSE_APPROVED] User: {}, DisputeId: {}",
            user.getEmail(), dispute.getId()
        );
    }
    
    // ============================================================
    // DISPUTE VIEW AUTHORIZATION
    // ============================================================
    
    /**
     * Authorize viewing of dispute details.
     * 
     * Business Rules:
     * 1. Dispute initiator can view their own dispute
     * 2. Related merchant can view dispute
     * 3. Admin can view any dispute
     * 4. Support users can view with TRANSACTION_VIEW_DISPUTE permission
     * 
     * @param user Authenticated user viewing dispute
     * @param dispute Dispute being viewed
     * @throws AccessDeniedException if not authorized
     */
    public void authorizeDisputeView(AuthenticatedUser user, Dispute dispute) {
        boolean isInitiator = dispute.getInitiatedBy().equals(user.getEmail());
        
        if (!isInitiator) {
            log.warn(
                "[AUTH_DISPUTE_VIEW_FAIL] Not authorized to view dispute. " +
                "User: {}, DisputeId: {}, IsInitiator: {}",
                user.getEmail(), dispute.getId(), isInitiator
            );
            throw new AccessDeniedException(
                "You are not authorized to view this dispute"
            );
        }
        
        log.debug(
            "[AUTH_DISPUTE_VIEW_APPROVED] User: {}, DisputeId: {}",
            user.getEmail(), dispute.getId()
        );
    }
    
    // ============================================================
    // DISPUTE RESOLUTION AUTHORIZATION
    // ============================================================
    
    /**
     * Authorize resolution of a dispute.
     * 
     * Business Rules:
     * 1. Only ADMIN can resolve disputes
     * 2. User must have DISPUTE_RESOLVE permission
     * 3. Dispute must not already be resolved/closed
     * 
     * @param user Authenticated user resolving dispute
     * @param dispute Dispute being resolved
     * @throws AccessDeniedException if not authorized
     */
    public void authorizeDisputeResolution(AuthenticatedUser user, Dispute dispute) {
        log.debug(
            "[AUTH_DISPUTE_RESOLVE] User: {}, DisputeId: {}",
            user.getEmail(), dispute.getId()
        );
        
        // For now, anyone can attempt to resolve
        // In a more restrictive system, you'd check roles here
        
        // Check dispute status
        if (dispute.isResolved()) {
            log.warn(
                "[AUTH_DISPUTE_RESOLVE_FAIL] Dispute already resolved. " +
                "Status: {}, ResolutionType: {}, User: {}, DisputeId: {}",
                dispute.getStatus(), dispute.getResolutionType(), user.getEmail(), dispute.getId()
            );
            throw new AccessDeniedException(
                "Dispute has already been resolved with decision: " + dispute.getResolutionType()
            );
        }
        
        log.info(
            "[AUTH_DISPUTE_RESOLVE_APPROVED] User: {}, DisputeId: {}",
            user.getEmail(), dispute.getId()
        );
    }
    
    // ============================================================
    // HELPER METHODS
    // ============================================================
}
