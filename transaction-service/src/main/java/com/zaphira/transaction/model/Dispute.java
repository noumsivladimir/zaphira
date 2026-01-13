package com.zaphira.transaction.model;

import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.model.enums.DisputeCategory;
import com.zaphira.transaction.model.enums.DisputeInitiatorRole;
import com.zaphira.transaction.model.enums.DisputeResolutionType;
import com.zaphira.transaction.model.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Dispute Entity - Represents a dispute raised against a transaction.
 * 
 * A dispute occurs when a customer or merchant contests a transaction.
 * Disputes go through a lifecycle: INITIATED → INVESTIGATION → EVIDENCE → RESPONSE → RESOLVED → CLOSED
 * 
 * Responsibilities:
 * 1. Track dispute details (category, reason, amounts)
 * 2. Maintain dispute status and timeline
 * 3. Link to original transaction
 * 4. Store evidence and resolution information
 * 5. Hold disputed amounts in escrow during investigation
 */
@Entity
@Table(name = "disputes", indexes = {
    @Index(name = "idx_disputes_transaction", columnList = "transaction_id"),
    @Index(name = "idx_disputes_status", columnList = "status"),
    @Index(name = "idx_disputes_created_at", columnList = "created_at DESC"),
    @Index(name = "idx_disputes_initiator", columnList = "initiated_by"),
    @Index(name = "idx_disputes_reference", columnList = "reference")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"transaction", "evidences", "timeline"})
@ToString(exclude = {"transaction", "evidences", "timeline"})
public class Dispute {
    
    // ============================================================
    // PRIMARY KEY & IDENTIFIERS
    // ============================================================
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique dispute reference for customer-facing communication
     * Format: DSP-{UUID} (e.g., DSP-a1b2c3d4e5f6g7h8)
     */
    @Column(name = "reference", unique = true, nullable = false, length = 50)
    private String reference;
    
    // ============================================================
    // TRANSACTION LINK
    // ============================================================
    
    /**
     * ID of the original transaction being disputed
     */
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;
    
    /**
     * Foreign key relationship to Transaction
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", insertable = false, updatable = false)
    private Transaction transaction;
    
    // ============================================================
    // DISPUTE CLASSIFICATION
    // ============================================================
    
    /**
     * Category of the dispute (e.g., FRAUD, DUPLICATE_CHARGE, SERVICE_NOT_PROVIDED)
     * Used for routing and resolution rules
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private DisputeCategory category;
    
    /**
     * Current status of the dispute in its lifecycle
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DisputeStatus status;
    
    // ============================================================
    // DISPUTE DETAILS
    // ============================================================
    
    /**
     * Reason for the dispute (required, up to 1000 chars)
     * Examples: "I didn't authorize this transaction", "Wrong amount charged"
     */
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;
    
    /**
     * Detailed description of the dispute (optional)
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * Amount being disputed (may differ from original transaction amount)
     */
    @Column(name = "claimed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal claimedAmount;
    
    /**
     * Currency of the disputed amount
     */
    @Column(name = "claimed_currency", length = 3)
    private String claimedCurrency;
    
    // ============================================================
    // INITIATOR INFORMATION
    // ============================================================
    
    /**
     * Email or User ID of who initiated the dispute
     */
    @Column(name = "initiated_by", nullable = false, length = 255)
    private String initiatedBy;
    
    /**
     * Role of the person who initiated the dispute
     * CUSTOMER: Buyer/sender disputes a payment
     * MERCHANT: Seller/receiver reports non-payment or fraud
     * ADMIN: Support staff initiated on behalf
     * SYSTEM: System auto-escalated
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "initiator_role", nullable = false, length = 50)
    private DisputeInitiatorRole initiatorRole;
    
    /**
     * Wallet number of the initiator (if applicable)
     */
    @Column(name = "initiated_by_wallet", length = 50)
    private String initiatedByWallet;
    
    // ============================================================
    // RESOLUTION INFORMATION
    // ============================================================
    
    /**
     * How the dispute was resolved
     * APPROVED: Refund customer, DENIED: Refund merchant, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", length = 50)
    private DisputeResolutionType resolutionType;
    
    /**
     * Admin/Team member who resolved the dispute
     */
    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;
    
    /**
     * Reason for the resolution decision
     */
    @Column(name = "resolution_reason", columnDefinition = "TEXT")
    private String resolutionReason;
    
    /**
     * Final amount to be refunded based on resolution
     * May be different from claimed amount
     */
    @Column(name = "resolution_amount", precision = 19, scale = 4)
    private BigDecimal resolutionAmount;
    
    // ============================================================
    // TIMELINE & DEADLINES
    // ============================================================
    
    /**
     * When the dispute was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Last time any field was updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Deadline for action (default: 30 days from creation)
     * After this date, dispute expires if not resolved
     */
    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;
    
    /**
     * When the dispute was finally resolved
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    // ============================================================
    // ACTIVITY TRACKING
    // ============================================================
    
    /**
     * Number of evidence items submitted so far
     */
    @Column(name = "evidence_submitted_count")
    private Integer evidenceSubmittedCount;
    
    /**
     * Timestamp of the most recent evidence submission
     */
    @Column(name = "last_evidence_submitted_at")
    private LocalDateTime lastEvidenceSubmittedAt;
    
    /**
     * Number of appeals requested by customer
     */
    @Column(name = "appeal_count")
    private Integer appealCount;
    
    // ============================================================
    // ADDITIONAL DATA
    // ============================================================
    
    /**
     * Additional context stored as JSON
     * Examples: {"escalationReason": "...", "notes": "..."}
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
    
    // ============================================================
    // RELATIONSHIPS
    // ============================================================
    
    /**
     * All evidence submitted for this dispute
     * One-to-many relationship with DisputeEvidence
     */
    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<DisputeEvidence> evidences = new HashSet<>();
    
    /**
     * Timeline events for this dispute
     * One-to-many relationship with DisputeTimeline
     */
    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<DisputeTimeline> timeline = new HashSet<>();
    
    // ============================================================
    // AUDIT INFORMATION
    // ============================================================
    
    /**
     * IP address of the initiator (for security audit)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User-Agent of the initiator's browser
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Request ID for distributed tracing across services
     */
    @Column(name = "request_id", length = 100)
    private String requestId;
    
    // ============================================================
    // LIFECYCLE HOOKS
    // ============================================================
    
    @PrePersist
    protected void onCreate() {
        // Generate unique reference
        if (this.reference == null) {
            this.reference = "DSP-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        }
        
        // Initialize status
        if (this.status == null) {
            this.status = DisputeStatus.INITIATED;
        }
        
        // Set default deadline (30 days)
        if (this.deadlineAt == null) {
            this.deadlineAt = LocalDateTime.now().plusDays(30);
        }
        
        // Initialize counters
        if (this.evidenceSubmittedCount == null) {
            this.evidenceSubmittedCount = 0;
        }
        if (this.appealCount == null) {
            this.appealCount = 0;
        }
    }
    
    // ============================================================
    // HELPER METHODS
    // ============================================================
    
    /**
     * Check if dispute is still open (can receive evidence/responses)
     */
    public boolean isOpenForInput() {
        return this.status == DisputeStatus.INITIATED || 
               this.status == DisputeStatus.AWAITING_EVIDENCE ||
               this.status == DisputeStatus.AWAITING_RESPONSE;
    }
    
    /**
     * Check if dispute deadline has passed
     */
    public boolean isDeadlinePassed() {
        return LocalDateTime.now().isAfter(this.deadlineAt);
    }
    
    /**
     * Check if dispute is resolved
     */
    public boolean isResolved() {
        return this.resolutionType != null;
    }
    
    /**
     * Add evidence to the dispute
     */
    public void addEvidence(DisputeEvidence evidence) {
        if (this.evidences == null) {
            this.evidences = new HashSet<>();
        }
        evidence.setDispute(this);
        this.evidences.add(evidence);
        this.evidenceSubmittedCount++;
        this.lastEvidenceSubmittedAt = LocalDateTime.now();
    }
    
    /**
     * Add timeline event
     */
    public void addTimelineEvent(DisputeTimeline event) {
        if (this.timeline == null) {
            this.timeline = new HashSet<>();
        }
        event.setDispute(this);
        this.timeline.add(event);
    }
}
