package com.zaphira.transaction.model;

import com.zaphira.transaction.model.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * DisputeTimeline Entity - Represents events in a dispute's lifecycle.
 * 
 * Creates an audit trail of all actions taken on a dispute:
 * - Creation of dispute
 * - Evidence submissions
 * - Status changes
 * - Resolution
 * - Appeals
 * 
 * Provides accountability and traceability for all dispute activities.
 * 
 * Responsibilities:
 * 1. Record all events in dispute lifecycle
 * 2. Track who performed each action and when
 * 3. Capture state transitions (old → new status)
 * 4. Enable chronological view of dispute history
 * 5. Support audit and compliance requirements
 */
@Entity
@Table(name = "dispute_timeline", indexes = {
    @Index(name = "idx_timeline_dispute", columnList = "dispute_id"),
    @Index(name = "idx_timeline_event_type", columnList = "event_type"),
    @Index(name = "idx_timeline_timestamp", columnList = "event_timestamp DESC"),
    @Index(name = "idx_timeline_actor_role", columnList = "actor_role")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "dispute")
@ToString(exclude = "dispute")
public class DisputeTimeline {
    
    // ============================================================
    // PRIMARY KEY & RELATIONSHIPS
    // ============================================================
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Foreign key to the Dispute this timeline event belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false, updatable = false)
    private Dispute dispute;
    
    // ============================================================
    // EVENT INFORMATION
    // ============================================================
    
    /**
     * Type of event that occurred
     * Examples: CREATED, EVIDENCE_ADDED, STATUS_CHANGED, RESOLVED, APPEALED, etc
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    /**
     * Detailed description of what happened in this event
     * Examples: "Customer submitted receipt as evidence", "Dispute status changed to UNDER_INVESTIGATION"
     */
    @Column(name = "event_description", columnDefinition = "TEXT")
    private String eventDescription;
    
    // ============================================================
    // ACTOR INFORMATION
    // ============================================================
    
    /**
     * Who performed this action
     * Examples: customer@example.com, admin@company.com, SYSTEM
     */
    @Column(name = "actor", nullable = false, length = 255)
    private String actor;
    
    /**
     * Role of the person performing the action
     * Possible values: CUSTOMER, MERCHANT, ADMIN, SYSTEM
     */
    @Column(name = "actor_role", length = 50)
    private String actorRole;
    
    // ============================================================
    // STATE TRANSITION INFORMATION
    // ============================================================
    
    /**
     * Previous status (if this is a status change event)
     * Examples: "INITIATED", "AWAITING_EVIDENCE", null if N/A
     */
    @Column(name = "old_status", length = 50)
    private String oldStatus;
    
    /**
     * New status (if this is a status change event)
     * Examples: "UNDER_INVESTIGATION", "RESOLVED", null if N/A
     */
    @Column(name = "new_status", length = 50)
    private String newStatus;
    
    // ============================================================
    // TIMING INFORMATION
    // ============================================================
    
    /**
     * Timestamp when this event occurred
     * Used to reconstruct dispute history chronologically
     */
    @CreationTimestamp
    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private LocalDateTime eventTimestamp;
    
    // ============================================================
    // AUDIT INFORMATION (Optional but recommended)
    // ============================================================
    
    /**
     * IP address of the actor (for security audit)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User-Agent of actor's browser
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Request ID for distributed tracing
     */
    @Column(name = "request_id", length = 100)
    private String requestId;
    
    // ============================================================
    // LIFECYCLE HOOKS
    // ============================================================
    
    @PrePersist
    protected void onCreate() {
        // Timestamp is set by @CreationTimestamp
    }
    
    // ============================================================
    // EVENT TYPE CONSTANTS
    // ============================================================
    
    public static final String EVENT_DISPUTE_CREATED = "DISPUTE_CREATED";
    public static final String EVENT_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String EVENT_EVIDENCE_ADDED = "EVIDENCE_ADDED";
    public static final String EVENT_RESPONSE_SUBMITTED = "RESPONSE_SUBMITTED";
    public static final String EVENT_DISPUTE_RESOLVED = "DISPUTE_RESOLVED";
    public static final String EVENT_APPEAL_REQUESTED = "APPEAL_REQUESTED";
    public static final String EVENT_ESCALATED = "ESCALATED";
    public static final String EVENT_DEADLINE_APPROACHING = "DEADLINE_APPROACHING";
    public static final String EVENT_EXPIRED = "EXPIRED";
    
    // ============================================================
    // HELPER METHODS
    // ============================================================
    
    /**
     * Create a timeline event for dispute creation
     */
    public static DisputeTimeline ofDisputeCreated(Dispute dispute, String actor, String actorRole) {
        return DisputeTimeline.builder()
            .dispute(dispute)
            .eventType(EVENT_DISPUTE_CREATED)
            .eventDescription("Dispute created for transaction " + dispute.getTransactionId())
            .actor(actor)
            .actorRole(actorRole)
            .newStatus(dispute.getStatus().name())
            .build();
    }
    
    /**
     * Create a timeline event for status change
     */
    public static DisputeTimeline ofStatusChanged(
        Dispute dispute, 
        DisputeStatus oldStatus, 
        DisputeStatus newStatus,
        String actor, 
        String actorRole,
        String reason
    ) {
        return DisputeTimeline.builder()
            .dispute(dispute)
            .eventType(EVENT_STATUS_CHANGED)
            .eventDescription("Status changed from " + oldStatus.getDescription() + 
                            " to " + newStatus.getDescription() + 
                            (reason != null ? ". Reason: " + reason : ""))
            .actor(actor)
            .actorRole(actorRole)
            .oldStatus(oldStatus.name())
            .newStatus(newStatus.name())
            .build();
    }
    
    /**
     * Create a timeline event for evidence submission
     */
    public static DisputeTimeline ofEvidenceAdded(
        Dispute dispute,
        String actor,
        String actorRole,
        String evidenceType
    ) {
        return DisputeTimeline.builder()
            .dispute(dispute)
            .eventType(EVENT_EVIDENCE_ADDED)
            .eventDescription("Evidence of type " + evidenceType + " added to dispute")
            .actor(actor)
            .actorRole(actorRole)
            .build();
    }
    
    /**
     * Create a timeline event for dispute resolution
     */
    public static DisputeTimeline ofDisputeResolved(
        Dispute dispute,
        String actor,
        String actorRole,
        String resolutionType,
        String reason
    ) {
        return DisputeTimeline.builder()
            .dispute(dispute)
            .eventType(EVENT_DISPUTE_RESOLVED)
            .eventDescription("Dispute resolved with decision: " + resolutionType + 
                            (reason != null ? ". Reason: " + reason : ""))
            .actor(actor)
            .actorRole(actorRole)
            .newStatus(DisputeStatus.RESOLVED.name())
            .build();
    }
}
