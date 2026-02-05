package com.zaphira.transaction.controller;

import com.zaphira.common.exception.ResourceNotFoundException;
import com.zaphira.common.exception.BusinessException;
import com.zaphira.transaction.dto.requests.DisputeEscalationRequest;
import com.zaphira.transaction.dto.requests.DisputeRequest;
import com.zaphira.transaction.dto.requests.DisputeResolutionRequest;
import com.zaphira.transaction.dto.requests.EvidenceRequest;
import com.zaphira.transaction.dto.response.DisputeMessageDTO;
import com.zaphira.transaction.dto.response.DisputeResponse;
import com.zaphira.transaction.exception.AccessDeniedException;
import com.zaphira.transaction.exception.ValidationException;
import com.zaphira.transaction.model.Dispute;
import com.zaphira.transaction.model.DisputeEvidence;
import com.zaphira.transaction.model.DisputeTimeline;
import com.zaphira.transaction.service.DisputeService;
import com.zaphira.transaction.service.DisputeResolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
//
/**
 * DisputeController - REST API endpoints for dispute management.
 *
 * Endpoints:
 * 1. POST /api/disputes - Create dispute (CUSTOMER/ADMIN)
 * 2. POST /api/disputes/{id}/evidence - Submit evidence (CUSTOMER/MERCHANT/ADMIN)
 * 3. POST /api/disputes/{id}/respond - Merchant response (MERCHANT/ADMIN)
 * 4. GET /api/disputes/{id} - Get details (CUSTOMER/MERCHANT/ADMIN/SUPPORT)
 * 5. PUT /api/disputes/{id}/resolve - Resolve dispute (ADMIN)
 *
 * Authorization:
 * - @PreAuthorize("hasAuthority('...')") at Spring Security level
 * - Multi-level authorization via DisputeService and DisputeAuthorizationService
 * - JWT extraction via SecurityContextHolder
 *
 * Error Handling:
 * - 400: ValidationException, IllegalArgumentException
 * - 403: AccessDeniedException
 * - 404: ResourceNotFoundException
 * - 409: BusinessException
 * - 500: All other exceptions
 *
 * Patterns (from Phase 1 & 2):
 * - JWT extraction (implicit via @PreAuthorize)
 * - Comprehensive error handling
 * - Detailed logging
 * - Request/Response DTOs
 */
@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dispute Management", description = "APIs for managing transaction disputes")
@SecurityRequirement(name = "bearerAuth")
public class DisputeController {

    private final DisputeService disputeService;
    private final DisputeResolutionService resolutionService;

    // ============================================================
    // CREATE DISPUTE
    // ============================================================

    /**
     * Create a new dispute for a transaction.
     *
     * Customers initiate disputes when they believe a transaction is:
     * - Fraudulent/unauthorized
     * - Duplicate charge
     * - Service not provided
     * - Incorrect amount
     * - Other billing issues
     *
     * Authorization: CUSTOMER (own transactions) or ADMIN
     *
     * HTTP 201: Dispute created successfully
     * HTTP 400: Invalid dispute data
     * HTTP 403: Not authorized (not customer of transaction)
     * HTTP 404: Transaction not found
     * HTTP 409: Dispute already exists for transaction
     * HTTP 500: Server error
     *
     * @param request DisputeRequest with transaction ID, category, reason, amount
     * @return 201 Created with DisputeResponse
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')")
    @Operation(
        summary = "Create a new dispute",
        description = "Customers can create disputes for their transactions"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Dispute created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DisputeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid dispute data"),
        @ApiResponse(responseCode = "403", description = "Not authorized to create dispute"),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "409", description = "Dispute already exists for transaction")
    })
    public ResponseEntity<DisputeResponse> createDispute(
        @Valid @RequestBody DisputeRequest request
    ) {
        log.info("[DISPUTE_CREATE_REQUEST] TransactionId: {}, Category: {}",
            request.getTransactionId(), request.getCategory());

        try {
            Dispute dispute = disputeService.createDispute(request);
            DisputeResponse response = mapToDisputeResponse(dispute);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ValidationException e) {
            log.warn("[DISPUTE_CREATE_VALIDATION_ERROR] {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException e) {
            log.warn("[DISPUTE_CREATE_NOT_FOUND] {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            log.error("[DISPUTE_CREATE_CONFLICT] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (AccessDeniedException e) {
            log.warn("[DISPUTE_CREATE_FORBIDDEN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("[DISPUTE_CREATE_ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================
    // SUBMIT EVIDENCE
    // ============================================================

    /**
     * Submit evidence for a dispute (multipart form data).
     *
     * Both customers and merchants can submit evidence:
     * - Customers: Receipts, screenshots, proof of unauthorized transaction
     * - Merchants: Delivery proof, communication records, invoices
     *
     * Authorization: CUSTOMER/MERCHANT for own/related disputes
     *
     * HTTP 200: Evidence submitted successfully
     * HTTP 400: Invalid file or request
     * HTTP 403: Not authorized to submit evidence
     * HTTP 404: Dispute not found
     * HTTP 500: Server error
     *
     * @param disputeId ID of the dispute
     * @param file File to upload (max 50MB)
     * @param evidenceType Type of evidence
     * @param description Optional description
     * @return 200 OK with evidence details
     */
    @PostMapping(value = "/{disputeId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_MERCHANT', 'ROLE_ADMIN')")
    @Operation(
        summary = "Submit evidence for a dispute",
        description = "Upload evidence files (receipts, screenshots, etc) to support dispute claim"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Evidence submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or request"),
        @ApiResponse(responseCode = "403", description = "Not authorized to submit evidence"),
        @ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    public ResponseEntity<?> submitEvidence(
        @PathVariable String disputeId,
        @RequestParam("file") MultipartFile file,
        @RequestParam("evidence_type") String evidenceType,
        @RequestParam(value = "description", required = false) String description
    ) {
        log.info("[EVIDENCE_SUBMIT_REQUEST] DisputeId: {}, EvidenceType: {}",
            disputeId, evidenceType);

        try {
            EvidenceRequest request = EvidenceRequest.builder()
                .file(file)
                .evidenceType(evidenceType)
                .description(description)
                .build();

            DisputeEvidence evidence = disputeService.submitEvidence(disputeId, request);

            return ResponseEntity.ok(Map.of(
                "message", "Evidence submitted successfully",
                "evidenceId", evidence.getId().toString(),
                "fileName", evidence.getFileName(),
                "submittedAt", evidence.getSubmittedAt()
            ));
        } catch (ValidationException e) {
            log.warn("[EVIDENCE_VALIDATION_ERROR] {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException e) {
            log.warn("[EVIDENCE_NOT_FOUND] {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            log.warn("[EVIDENCE_FORBIDDEN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("[EVIDENCE_ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================
    // MERCHANT RESPONSE
    // ============================================================

    /**
     * Merchant response to dispute with counter-evidence.
     *
     * Merchants can defend against disputes by providing:
     * - Delivery proof
     * - Communication records
     * - Service completion evidence
     * - Other supporting documentation
     *
     * Authorization: MERCHANT for related disputes
     *
     * HTTP 200: Response submitted successfully
     * HTTP 400: Invalid request
     * HTTP 403: Not authorized
     * HTTP 404: Dispute not found
     * HTTP 500: Server error
     *
     * @param disputeId ID of the dispute
     * @param request EvidenceRequest with counter-evidence
     * @return 200 OK with updated dispute
     */
    @PostMapping(value = "/{disputeId}/respond", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_MERCHANT', 'ROLE_ADMIN')")
    @Operation(
        summary = "Respond to dispute with counter-evidence",
        description = "Merchants can submit counter-evidence to defend against dispute"
    )
    public ResponseEntity<?> respondToDispute(
        @PathVariable String disputeId,
        @RequestParam("file") MultipartFile file,
        @RequestParam("evidence_type") String evidenceType,
        @RequestParam(value = "description", required = false) String description
    ) {
        log.info("[DISPUTE_RESPONSE_REQUEST] DisputeId: {}", disputeId);

        try {
            EvidenceRequest request = EvidenceRequest.builder()
                .file(file)
                .evidenceType(evidenceType)
                .description(description)
                .build();

            Dispute updatedDispute = disputeService.respondToDispute(disputeId, request);
            DisputeResponse response = mapToDisputeResponse(updatedDispute);

            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            log.warn("[DISPUTE_RESPONSE_FORBIDDEN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (ResourceNotFoundException e) {
            log.warn("[DISPUTE_RESPONSE_NOT_FOUND] {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("[DISPUTE_RESPONSE_ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================
    // GET DISPUTE DETAILS
    // ============================================================

    /**
     * Get full dispute details with evidence and timeline.
     *
     * Returns:
     * - Dispute summary and status
     * - All submitted evidence (file metadata)
     * - Timeline of all events
     * - Current deadline info
     *
     * Authorization: Dispute initiator, related parties, or admin
     *
     * HTTP 200: Dispute details
     * HTTP 403: Not authorized to view
     * HTTP 404: Dispute not found
     * HTTP 500: Server error
     *
     * @param disputeId ID of the dispute
     * @return 200 OK with DisputeResponse
     */
    @GetMapping("/{disputeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_MERCHANT', 'ROLE_ADMIN', 'ROLE_SUPPORT')")
    @Operation(
        summary = "Get dispute details",
        description = "Retrieve full dispute information including evidence and timeline"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dispute details retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DisputeResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized to view dispute"),
        @ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    public ResponseEntity<?> getDisputeDetails(
        @PathVariable String disputeId
    ) {
        log.info("[DISPUTE_GET_REQUEST] DisputeId: {}", disputeId);

        try {
            Dispute dispute = disputeService.getDisputeDetails(disputeId);
            DisputeResponse response = mapToDisputeResponse(dispute);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            log.warn("[DISPUTE_GET_FORBIDDEN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (ResourceNotFoundException e) {
            log.warn("[DISPUTE_GET_NOT_FOUND] {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("[DISPUTE_GET_ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================
    // RESOLVE DISPUTE (ADMIN ONLY)
    // ============================================================

    /**
     * Resolve a dispute with final decision and fund release.
     *
     * Admins make final decisions:
     * - APPROVED: Refund to customer
     * - PARTIAL_APPROVAL: Split refund
     * - DENIED: Release to merchant
     * - SETTLEMENT: Negotiated amount
     * - WITHDRAWN: Customer withdraws claim
     * - EXPIRED: Deadline passed
     * - ESCALATED_TO_BANK: Escalate to payment processor
     *
     * Authorization: ADMIN only (requires DISPUTE_RESOLVE permission)
     *
     * HTTP 200: Dispute resolved successfully
     * HTTP 400: Invalid resolution data
     * HTTP 403: Not authorized (not admin)
     * HTTP 404: Dispute not found
     * HTTP 409: Dispute already resolved
     * HTTP 500: Server error
     *
     * @param disputeId ID of the dispute
     * @param request DisputeResolutionRequest with decision
     * @return 200 OK with resolved dispute
     */
    @PutMapping("/{disputeId}/resolve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
        summary = "Resolve dispute (Admin only)",
        description = "Make final decision on dispute: APPROVED, DENIED, PARTIAL_APPROVAL, SETTLEMENT, etc"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dispute resolved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid resolution data"),
        @ApiResponse(responseCode = "403", description = "Not authorized (admin only)"),
        @ApiResponse(responseCode = "404", description = "Dispute not found"),
        @ApiResponse(responseCode = "409", description = "Dispute already resolved")
    })
    public ResponseEntity<?> resolveDispute(
        @PathVariable String disputeId,
        @Valid @RequestBody DisputeResolutionRequest request
    ) {
        log.info("[DISPUTE_RESOLVE_REQUEST] DisputeId: {}, ResolutionType: {}",
            disputeId, request.getResolutionType());

        try {
            request.setDisputeId(disputeId);
            Dispute resolvedDispute = resolutionService.resolveDispute(request);
            DisputeResponse response = mapToDisputeResponse(resolvedDispute);

            return ResponseEntity.ok(response);
        } catch (ValidationException e) {
            log.warn("[DISPUTE_RESOLVE_INVALID] {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException e) {
            log.warn("[DISPUTE_RESOLVE_NOT_FOUND] {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            log.warn("[DISPUTE_RESOLVE_INVALID] {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (AccessDeniedException e) {
            log.warn("[DISPUTE_RESOLVE_FORBIDDEN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("[DISPUTE_RESOLVE_ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Map Dispute entity to DisputeResponse DTO.
     */
    private DisputeResponse mapToDisputeResponse(Dispute dispute) {
        return DisputeResponse.builder()
            .disputeId(dispute.getId().toString())
            .transactionId(dispute.getTransactionId().toString())
            .status(dispute.getStatus().name())
            .category(dispute.getCategory().name())
            .reason(dispute.getReason())
            .claimedAmount(dispute.getClaimedAmount())
            .initiatedBy(dispute.getInitiatedBy())
            .initiatorRole(dispute.getInitiatorRole().name())
            .resolutionType(dispute.getResolutionType() != null ? dispute.getResolutionType().name() : null)
            .resolutionAmount(dispute.getResolutionAmount())
            .resolvedBy(dispute.getResolvedBy())
            .createdAt(dispute.getCreatedAt())
            .deadlineAt(dispute.getDeadlineAt())
            .resolvedAt(dispute.getResolvedAt())
            .build();
    }
    
    // ============================================================
    // LIST ALL DISPUTES
    // ============================================================
    
    /**
     * Get all disputes (filtered by user role).
     * 
     * GET /api/disputes
     * 
     * Authorization:
     * - ADMIN: Can see all disputes
     * - REGULAR/MERCHANT: Can only see disputes they initiated
     * 
     * Response: List of disputes
     * [
     *   {
     *     "disputeId": "123",
     *     "transactionId": "456",
     *     "status": "UNDER_INVESTIGATION",
     *     "category": "FRAUD",
     *     "reason": "Unauthorized transaction",
     *     "claimedAmount": 100.00,
     *     "initiatedBy": "customer@example.com",
     *     "createdAt": "2026-02-04T10:00:00",
     *     "deadlineAt": "2026-02-18T10:00:00"
     *   }
     * ]
     * 
     * @return List of disputes user is authorized to view
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('REGULAR', 'MERCHANT', 'ADMIN')")
    @Operation(summary = "Get all disputes", 
               description = "List all disputes (filtered by user role)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Disputes retrieved successfully",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = DisputeResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid/missing JWT"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<List<DisputeResponse>> getAllDisputes() {
        try {
            log.info("[DISPUTE_LIST_API] Fetching all disputes");
            
            List<Dispute> disputes = disputeService.getAllDisputes();
            List<DisputeResponse> response = disputes.stream()
                .map(this::mapToDisputeResponse)
                .collect(Collectors.toList());
            
            log.info("[DISPUTE_LIST_API_SUCCESS] Retrieved {} disputes", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[DISPUTE_LIST_API_ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ============================================================
    // GET DISPUTE MESSAGES/TIMELINE
    // ============================================================
    
    /**
     * Get dispute timeline/message history.
     * 
     * GET /api/disputes/{disputeId}/messages
     * 
     * Returns chronological list of all events in dispute lifecycle:
     * - Dispute creation
     * - Evidence submissions
     * - Status changes
     * - Merchant responses
     * - Admin actions
     * - Resolution
     * 
     * Response: List of timeline events
     * [
     *   {
     *     "id": 1,
     *     "eventType": "CREATED",
     *     "message": "Dispute created by customer",
     *     "actor": "customer@example.com",
     *     "actorRole": "CUSTOMER",
     *     "timestamp": "2026-02-04T10:00:00"
     *   },
     *   {
     *     "id": 2,
     *     "eventType": "EVIDENCE_ADDED",
     *     "message": "Customer submitted receipt as evidence",
     *     "actor": "customer@example.com",
     *     "actorRole": "CUSTOMER",
     *     "timestamp": "2026-02-04T11:30:00"
     *   }
     * ]
     * 
     * @param disputeId Dispute ID
     * @return List of timeline events ordered by timestamp
     */
    @GetMapping("/{disputeId}/messages")
    @PreAuthorize("hasAnyAuthority('REGULAR', 'MERCHANT', 'ADMIN')")
    @Operation(summary = "Get dispute message history", 
               description = "Returns chronological list of all events in dispute lifecycle")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages retrieved successfully",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = DisputeMessageDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid/missing JWT"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to view this dispute"),
        @ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    public ResponseEntity<List<DisputeMessageDTO>> getDisputeMessages(
            @PathVariable Long disputeId) {
        try {
            log.info("[DISPUTE_MESSAGES_API] Fetching messages for dispute: {}", disputeId);
            
            List<DisputeTimeline> timeline = disputeService.getDisputeMessages(disputeId);
            
            List<DisputeMessageDTO> messages = timeline.stream()
                .map(event -> DisputeMessageDTO.builder()
                    .id(event.getId())
                    .eventType(event.getEventType())
                    .message(event.getEventDescription())
                    .actor(event.getActor())
                    .actorRole(event.getActorRole())
                    .oldStatus(event.getOldStatus() != null ? event.getOldStatus().toString() : null)
                    .newStatus(event.getNewStatus() != null ? event.getNewStatus().toString() : null)
                    .timestamp(event.getEventTimestamp())
                    .build())
                .collect(Collectors.toList());
            
            log.info("[DISPUTE_MESSAGES_API_SUCCESS] Retrieved {} messages", messages.size());
            return ResponseEntity.ok(messages);
        } catch (ResourceNotFoundException e) {
            log.warn("[DISPUTE_MESSAGES_NOT_FOUND] {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            log.warn("[DISPUTE_MESSAGES_FORBIDDEN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("[DISPUTE_MESSAGES_ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ============================================================
    // ESCALATE DISPUTE
    // ============================================================
    
    /**
     * Escalate dispute to higher authority.
     * 
     * POST /api/disputes/{disputeId}/escalate
     * 
     * Request Body:
     * {
     *   "reason": "Customer unsatisfied with initial resolution",
     *   "priority": "HIGH",
     *   "additionalNotes": "Case involves significant amount"
     * }
     * 
     * Escalation occurs when:
     * - Customer unsatisfied with resolution
     * - Merchant disputes the decision
     * - Complex case requiring senior review
     * - Legal/compliance escalation needed
     * 
     * Changes status to ESCALATED and adds timeline event.
     * 
     * Authorization:
     * - CUSTOMER: Can escalate own disputes
     * - MERCHANT: Can escalate disputes for their transactions
     * - ADMIN: Can escalate any dispute
     * 
     * Response: Updated dispute with ESCALATED status
     * 
     * @param disputeId Dispute ID
     * @param request Escalation request with reason
     * @return Updated dispute
     */
    @PostMapping("/{disputeId}/escalate")
    @PreAuthorize("hasAnyAuthority('REGULAR', 'MERCHANT', 'ADMIN')")
    @Operation(summary = "Escalate dispute", 
               description = "Escalate dispute to higher authority when standard resolution is insufficient")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dispute escalated successfully",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = DisputeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request - Missing reason or invalid status"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid/missing JWT"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to escalate this dispute"),
        @ApiResponse(responseCode = "404", description = "Dispute not found")
    })
    public ResponseEntity<DisputeResponse> escalateDispute(
            @PathVariable Long disputeId,
            @Valid @RequestBody DisputeEscalationRequest request) {
        try {
            log.info("[DISPUTE_ESCALATE_API] Escalating dispute: {} with reason: {}", 
                    disputeId, request.getReason());
            
            Dispute escalatedDispute = disputeService.escalateDispute(disputeId, request.getReason());
            DisputeResponse response = mapToDisputeResponse(escalatedDispute);
            
            log.info("[DISPUTE_ESCALATE_API_SUCCESS] Dispute {} escalated to ESCALATED status", 
                    disputeId);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            log.warn("[DISPUTE_ESCALATE_NOT_FOUND] {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (ValidationException | IllegalArgumentException e) {
            log.warn("[DISPUTE_ESCALATE_INVALID] {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (AccessDeniedException e) {
            log.warn("[DISPUTE_ESCALATE_FORBIDDEN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("[DISPUTE_ESCALATE_ERROR] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
