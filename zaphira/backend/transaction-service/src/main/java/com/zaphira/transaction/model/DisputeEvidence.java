package com.zaphira.transaction.model;

import com.zaphira.transaction.model.enums.EvidenceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * DisputeEvidence Entity - Represents evidence submitted in support of a dispute.
 * 
 * Customers and merchants can submit evidence to support their position in a dispute.
 * Evidence types include: receipts, invoices, screenshots, communication records, etc.
 * 
 * Responsibilities:
 * 1. Track file uploads and metadata
 * 2. Store evidence type and descriptions
 * 3. Track who submitted evidence and when
 * 4. Track verification status (for admin review)
 * 5. Support audit trail with IP and user-agent
 */
@Entity
@Table(name = "dispute_evidence", indexes = {
    @Index(name = "idx_evidence_dispute", columnList = "dispute_id"),
    @Index(name = "idx_evidence_type", columnList = "evidence_type"),
    @Index(name = "idx_evidence_submitted_at", columnList = "submitted_at DESC"),
    @Index(name = "idx_evidence_verified", columnList = "verified")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "dispute")
@ToString(exclude = "dispute")
public class DisputeEvidence {
    
    // ============================================================
    // PRIMARY KEY & RELATIONSHIPS
    // ============================================================
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Foreign key to the Dispute this evidence belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false, updatable = false)
    private Dispute dispute;
    
    // ============================================================
    // EVIDENCE CLASSIFICATION
    // ============================================================
    
    /**
     * Type of evidence (RECEIPT, INVOICE, DELIVERY_PROOF, COMMUNICATION, etc)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 50)
    private EvidenceType evidenceType;
    
    // ============================================================
    // FILE INFORMATION
    // ============================================================
    
    /**
     * URL to the uploaded file (S3, Cloud Storage, etc)
     * Examples: s3://bucket/disputes/dsp-abc123/receipt-01.pdf
     */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;
    
    /**
     * Original filename provided by uploader
     * Examples: "receipt.pdf", "screenshot.png"
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    /**
     * File size in bytes
     */
    @Column(name = "file_size")
    private Long fileSize;
    
    /**
     * MIME type of the file
     * Examples: "image/png", "application/pdf", "text/plain"
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    
    // ============================================================
    // EVIDENCE DETAILS
    // ============================================================
    
    /**
     * Optional description of the evidence provided by submitter
     * Examples: "Invoice for order #12345", "Email conversation regarding refund"
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    // ============================================================
    // SUBMISSION INFORMATION
    // ============================================================
    
    /**
     * Email/User ID of who submitted this evidence
     */
    @Column(name = "submitted_by", nullable = false, length = 255)
    private String submittedBy;
    
    /**
     * Timestamp when evidence was uploaded
     */
    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;
    
    // ============================================================
    // VERIFICATION INFORMATION
    // ============================================================
    
    /**
     * Whether this evidence has been verified/reviewed by admin
     */
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;
    
    /**
     * Admin/Team member who verified this evidence
     */
    @Column(name = "verified_by", length = 255)
    private String verifiedBy;
    
    /**
     * When the evidence was verified
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    /**
     * Admin's assessment of evidence validity
     */
    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;
    
    // ============================================================
    // AUDIT INFORMATION
    // ============================================================
    
    /**
     * IP address of the submitter (for security audit)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User-Agent of submitter's browser
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
        if (this.verified == null) {
            this.verified = false;
        }
    }
    
    // ============================================================
    // HELPER METHODS
    // ============================================================
    
    /**
     * Mark this evidence as verified by admin
     */
    public void markAsVerified(String verifiedBy, String notes) {
        this.verified = true;
        this.verifiedBy = verifiedBy;
        this.verifiedAt = LocalDateTime.now();
        this.verificationNotes = notes;
    }
    
    /**
     * Check if file size is valid (< 50MB)
     */
    public boolean isValidFileSize() {
        long maxSize = 50 * 1024 * 1024; // 50 MB
        return fileSize != null && fileSize <= maxSize;
    }
}
