package com.zaphira.transaction.service;

import com.zaphira.common.exception.ResourceNotFoundException;
import com.zaphira.transaction.dto.requests.EvidenceRequest;
import com.zaphira.transaction.event.DisputeCreatedEvent;
import com.zaphira.transaction.exception.AccessDeniedException;
import com.zaphira.transaction.exception.ValidationException;
import com.zaphira.transaction.model.Dispute;
import com.zaphira.transaction.model.DisputeEvidence;
import com.zaphira.transaction.model.DisputeTimeline;
import com.zaphira.transaction.model.enums.DisputeInitiatorRole;
import com.zaphira.transaction.model.enums.DisputeStatus;
import com.zaphira.transaction.model.enums.EvidenceType;
import com.zaphira.transaction.repository.DisputeEvidenceRepository;
import com.zaphira.transaction.repository.DisputeRepository;
import com.zaphira.transaction.security.AuthenticatedUser;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DisputeService - Core service for managing disputes.
 * 
 * Responsibilities:
 * 1. Create disputes with validation and JWT extraction
 * 2. Submit evidence with file upload tracking
 * 3. Respond to disputes (defend or provide counter-evidence)
 * 4. Track dispute lifecycle and state transitions
 * 5. Publish dispute events to Kafka
 * 6. Maintain audit trail with detailed logging
 * 
 * Authorization:
 * - Customers: Can create disputes for their own transactions
 * - Merchants: Can respond to disputes and submit evidence
 * - Admins: Full access, can create disputes on behalf of others
 * 
 * Patterns (from Phase 1 & 2):
 * - JWT extraction via getAuthenticatedUser()
 * - Multi-level authorization via DisputeAuthorizationService
 * - Transactional consistency
 * - Kafka event publishing after state changes
 * - Comprehensive audit logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {
    
    private final DisputeRepository disputeRepository;
    private final DisputeEvidenceRepository evidenceRepository;
    private final TransactionServiceImpl transactionServiceImpl;
    private final DisputeAuthorizationService authorizationService;
    @Nullable
    private final KafkaTemplate<String, DisputeCreatedEvent> kafkaTemplate;
    
    @Value("${kafka.topics.dispute-created:dispute-created}")
    private String disputeCreatedTopic;
    
    @Value("${dispute.evidence.max-size:52428800}")
    private long maxFileSize; // 50 MB default
    
    @Value("${dispute.evidence-deadline-days:14}")
    private int evidenceDeadlineDays;
    
    // ============================================================
    // CREATE DISPUTE
    // ============================================================
    
    /**
     * Create a new dispute for a transaction.
     * 
     * Flow:
     * 1. Extract JWT to get authenticated user
     * 2. Get and validate transaction
     * 3. Check authorization (via DisputeAuthorizationService)
     * 4. Validate dispute request
     * 5. Create dispute entity with reference number
     * 6. Create initial timeline event
     * 7. Save to database
     * 8. Publish DisputeCreatedEvent to Kafka
     * 9. Log audit trail
     * 
     * @param request DisputeRequest with transactionId, category, reason, amount
     * @return Created Dispute with timeline
     * @throws ResourceNotFoundException if transaction not found
     * @throws AccessDeniedException if not authorized to dispute
     * @throws ValidationException if dispute data invalid
     * @throws OptimisticLockException if concurrent modification detected
     */
    @Transactional
    public Dispute createDispute(com.zaphira.transaction.dto.requests.DisputeRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();

        log.info(
            "[DISPUTE_CREATE_START] User: {}, TransactionId: {}, Amount: {}, RequestId: {}",
            user.getEmail(), request.getTransactionId(), request.getClaimedAmount(),
            request.toString().hashCode()
        );

        // Get and validate transaction
        com.zaphira.transaction.dto.TransactionDTO transactionDTO = transactionServiceImpl.getTransactionById(Long.parseLong(request.getTransactionId()));
        if (transactionDTO == null) {
            log.warn(
                "[DISPUTE_CREATE_FAIL] Transaction not found: {}, User: {}",
                request.getTransactionId(), user.getEmail()
            );
            throw new ResourceNotFoundException("Transaction not found: " + request.getTransactionId());
        }

        // Check if dispute already exists
        if (disputeRepository.existsByTransactionId(Long.parseLong(request.getTransactionId()))) {
            log.warn(
                "[DISPUTE_CREATE_FAIL] Dispute already exists for transaction: {}, User: {}",
                request.getTransactionId(), user.getEmail()
            );
            throw new ValidationException("Dispute already exists for this transaction");
        }

        // Authorize dispute creation
        authorizationService.authorizeDisputeCreation(user, transactionDTO, request);

        // Validate dispute amount
        if (request.getClaimedAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Claimed amount must be greater than 0");
        }
        if (request.getClaimedAmount().compareTo(transactionDTO.getAmount()) > 0) {
            throw new ValidationException(
                "Claimed amount cannot exceed transaction amount of " + transactionDTO.getAmount()
            );
        }

        // Create dispute entity
        Dispute dispute = Dispute.builder()
            .transactionId(transactionDTO.getId())
            .reference("DSP-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
            .category(com.zaphira.transaction.model.enums.DisputeCategory.valueOf(request.getCategory().toUpperCase()))
            .status(DisputeStatus.INITIATED)
            .reason(request.getReason())
            .description(request.getDescription())
            .claimedAmount(request.getClaimedAmount())
            .initiatedBy(user.getEmail())
            .initiatorRole(DisputeInitiatorRole.CUSTOMER)
            .deadlineAt(LocalDateTime.now().plusDays(evidenceDeadlineDays))
            .createdAt(LocalDateTime.now())
            .build();

        // Save dispute
        Dispute savedDispute = disputeRepository.save(dispute);

        // Create initial timeline event
        DisputeTimeline timelineEvent = DisputeTimeline.ofDisputeCreated(
            savedDispute,
            user.getEmail(),
            DisputeInitiatorRole.CUSTOMER.name()
        );

        savedDispute.getTimeline().add(timelineEvent);
        savedDispute = disputeRepository.save(savedDispute);

        // Publish event to Kafka
        publishDisputeCreatedEvent(savedDispute, user);

        log.info(
            "[DISPUTE_CREATE_SUCCESS] DisputeId: {}, TransactionId: {}, Amount: {}, User: {}",
            savedDispute.getId(), savedDispute.getTransactionId(),
            savedDispute.getClaimedAmount(), user.getEmail()
        );

        return savedDispute;
    }

    // ============================================================
    // SUBMIT EVIDENCE
    // ============================================================
    
    /**
     * Submit evidence for a dispute.
     * 
     * Flow:
     * 1. Extract JWT and get authenticated user
     * 2. Get dispute and validate it exists
     * 3. Check authorization (via DisputeAuthorizationService)
     * 4. Validate evidence file
     * 5. Create DisputeEvidence entity
     * 6. Save evidence and create timeline event
     * 7. Update dispute status if needed
     * 8. Log audit trail
     * 
     * @param disputeId ID of the dispute
     * @param request EvidenceRequest with file and metadata
     * @return Created DisputeEvidence
     * @throws ResourceNotFoundException if dispute not found
     * @throws AccessDeniedException if not authorized to submit
     * @throws ValidationException if evidence invalid
     */
//    @Transactional
    public DisputeEvidence submitEvidence(String disputeId, EvidenceRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        
        log.info(
            "[EVIDENCE_SUBMIT_START] User: {}, DisputeId: {}, EvidenceType: {}",
            user.getEmail(), disputeId, request.getEvidenceType()
        );
        
        // Get dispute
        Dispute dispute = disputeRepository.findById(Long.parseLong(disputeId))
            .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));
        
        // Check authorization
        authorizationService.authorizeEvidenceSubmission(user, dispute);
        
        // Validate dispute is open for input
        if (!dispute.isOpenForInput()) {
            throw new ValidationException("Dispute is not open for evidence submission");
        }
        
        // Validate evidence deadline
        if (dispute.isDeadlinePassed()) {
            log.warn(
                "[EVIDENCE_SUBMIT_FAIL] Evidence deadline passed for dispute: {}, User: {}",
                disputeId, user.getEmail()
            );
            throw new ValidationException("Evidence submission deadline has passed");
        }
        
        // Validate file
        validateEvidenceFile(request.getFile());
        
        // Create evidence entity
        DisputeEvidence evidence = DisputeEvidence.builder()
            .dispute(dispute)
            .evidenceType(EvidenceType.valueOf(request.getEvidenceType()))
            .fileName(request.getFile().getOriginalFilename())
            .fileSize(request.getFile().getSize())
            .mimeType(request.getFile().getContentType())
            .description(request.getDescription())
            .submittedBy(user.getEmail())
            .submittedAt(LocalDateTime.now())
            .verified(false)
            // TODO: Upload file to S3/Cloud Storage and set fileUrl
            .fileUrl("s3://evidence-bucket/disputes/" + dispute.getReference() + "/" + 
                    UUID.randomUUID().toString() + "-" + request.getFile().getOriginalFilename())
            .build();
        
        // Save evidence
        DisputeEvidence savedEvidence = evidenceRepository.save(evidence);
        
        // Create timeline event
        DisputeTimeline timelineEvent = DisputeTimeline.ofEvidenceAdded(
            dispute,
            user.getEmail(),
            DisputeInitiatorRole.CUSTOMER.name(),
            request.getEvidenceType()
        );
        
        dispute.getTimeline().add(timelineEvent);
        disputeRepository.save(dispute);
        
        log.info(
            "[EVIDENCE_SUBMIT_SUCCESS] EvidenceId: {}, DisputeId: {}, Type: {}, Size: {}, User: {}",
            savedEvidence.getId(), disputeId, request.getEvidenceType(),
            request.getFile().getSize(), user.getEmail()
        );
        
        return savedEvidence;
    }
    
    // ============================================================
    // RESPOND TO DISPUTE
    // ============================================================
    
    /**
     * Merchant or defendant responds to a dispute.
     * 
     * Allows merchant to provide counter-evidence or explanation.
     * Updates dispute status to AWAITING_RESPONSE → tracked by timeline.
     * 
     * @param disputeId ID of the dispute
     * @param request EvidenceRequest with response evidence
     * @return Updated Dispute
     */
    @Transactional
    public Dispute respondToDispute(String disputeId, EvidenceRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        
        log.info(
            "[DISPUTE_RESPOND_START] User: {}, DisputeId: {}, EvidenceType: {}",
            user.getEmail(), disputeId, request.getEvidenceType()
        );
        
        // Get dispute
        Dispute dispute = disputeRepository.findById(Long.parseLong(disputeId))
            .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));
        
        // Check authorization (merchant can respond)
        authorizationService.authorizeDisputeResponse(user, dispute);
        
        // Submit evidence
        submitEvidence(disputeId, request);
        
        // Update status to AWAITING_RESPONSE if still in INITIATED
        if (dispute.getStatus() == DisputeStatus.INITIATED) {
            dispute.setStatus(DisputeStatus.AWAITING_RESPONSE);
            
            DisputeTimeline timelineEvent = DisputeTimeline.ofStatusChanged(
                dispute,
                DisputeStatus.INITIATED,
                DisputeStatus.AWAITING_RESPONSE,
                user.getEmail(),
                "MERCHANT",
                "Merchant response submitted"
            );
            
            dispute.getTimeline().add(timelineEvent);
            dispute = disputeRepository.save(dispute);
        }
        
        log.info(
            "[DISPUTE_RESPOND_SUCCESS] DisputeId: {}, User: {}, Status: {}",
            disputeId, user.getEmail(), dispute.getStatus()
        );
        
        return dispute;
    }
    
    // ============================================================
    // GET DISPUTE DETAILS
    // ============================================================
    
    /**
     * Get full dispute details with evidence and timeline.
     * 
     * Applies authorization - users can only see their own disputes
     * unless they are admins.
     * 
     * @param disputeId ID of the dispute
     * @return Dispute with all details
     * @throws ResourceNotFoundException if dispute not found
     * @throws AccessDeniedException if not authorized
     */
    @Transactional
    public Dispute getDisputeDetails(String disputeId) {
        AuthenticatedUser user = getAuthenticatedUser();
        
        Dispute dispute = disputeRepository.findById(Long.parseLong(disputeId))
            .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));
        
        // Authorize view
        authorizationService.authorizeDisputeView(user, dispute);
        
        return dispute;
    }
    
    // ============================================================
    // HELPER METHODS
    // ============================================================
    
    /**
     * Extract authenticated user from SecurityContext (JWT extraction).
     * 
     * Pattern from Phase 1 & 2.
     * Validates that:
     * 1. SecurityContext is set
     * 2. Authentication exists
     * 3. Principal is AuthenticatedUser type
     * 
     * @return AuthenticatedUser from JWT
     * @throws IllegalStateException if no valid authentication
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
     * Validate evidence file before upload.
     * 
     * @param file File to validate
     * @throws ValidationException if file invalid
     */
    private void validateEvidenceFile(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is required and cannot be empty");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new ValidationException(
                "File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + " MB"
            );
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new ValidationException("File type not allowed: " + contentType);
        }
    }
    
    /**
     * Check if content type is allowed for evidence.
     */
    private boolean isAllowedContentType(String contentType) {
        return contentType.matches("(image/.*|application/pdf|text/.*|application/.*word.*|application/.*excel.*)");
    }
    
    /**
     * Publish DisputeCreatedEvent to Kafka.
     */
    private void publishDisputeCreatedEvent(Dispute dispute, AuthenticatedUser user) {
        if (kafkaTemplate == null) {
            log.warn("[KAFKA_SKIP] KafkaTemplate not available, skipping event publish for dispute: {}", dispute.getReference());
            return;
        }
        
        DisputeCreatedEvent event = DisputeCreatedEvent.builder()
            .disputeId(dispute.getId())
            .reference(dispute.getReference())
            .transactionId(String.valueOf(dispute.getTransactionId()))
            .category(dispute.getCategory().name())
            .reason(dispute.getReason())
            .claimedAmount(dispute.getClaimedAmount())
            .initiatedBy(user.getEmail())
            .initiatorRole(dispute.getInitiatorRole().name())
            .createdAt(dispute.getCreatedAt())
            .deadlineAt(dispute.getDeadlineAt())
            .build();
        
        kafkaTemplate.send(disputeCreatedTopic, dispute.getReference(), event);
        log.info("[KAFKA_PUBLISH] DisputeCreatedEvent sent for dispute: {}", dispute.getReference());
    }
}
