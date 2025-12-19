# 📋 PHASE 3: DISPUTE MANAGEMENT - IMPLÉMENTATION COMPLÈTE
**Date:** 16 Décembre 2025  
**Status:** 🔄 IN PROGRESS  
**Coherence Level:** Adaptée des Phases 1 & 2  
**Expected Completion:** 92% (Code 100%, Testing 90%, Deployment 0%)

---

## 🎯 VISION GLOBALE PHASE 3

```
┌─────────────────────────────────────────────────────────────┐
│         DISPUTE MANAGEMENT SYSTEM ARCHITECTURE              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [Dispute Initiation]                                       │
│        ↓                                                    │
│  [DisputeController] → POST /disputes (JWT auth)           │
│        ↓                                                    │
│  [DisputeService] → Create + Authorize + Log               │
│        ↓                                                    │
│  [DisputeAuthorizationService] → Multi-level auth          │
│        ↓                                                    │
│  [WalletService] → Hold disputed amount (freeze)           │
│        ↓                                                    │
│  [Kafka Event] → DisputeCreatedEvent published             │
│        ↓                                                    │
│  [Dispute Resolution]                                      │
│        ├─ POST /disputes/{id}/evidence (Upload proof)      │
│        ├─ POST /disputes/{id}/respond (Defendant reply)    │
│        ├─ PUT /disputes/{id}/status (Investigation)        │
│        └─ PUT /disputes/{id}/resolve (Final decision)      │
│        ↓                                                    │
│  [Release Funds] → Based on decision (APPROVED/DENIED)     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 COMPOSANTS À IMPLÉMENTER

### 1. MODELS & ENTITIES ✅ (À créer)
- [ ] Dispute.java
- [ ] DisputeEvidence.java
- [ ] DisputeTimeline.java
- [ ] DisputeResolution.java

### 2. ENUMS ✅ (À créer)
- [ ] DisputeStatus.java
- [ ] DisputeCategory.java
- [ ] DisputeResolutionType.java
- [ ] EvidenceType.java

### 3. DTOs ✅ (À créer)
- [ ] DisputeRequest.java
- [ ] DisputeResponse.java
- [ ] EvidenceRequest.java
- [ ] DisputeResolutionRequest.java

### 4. SERVICES ✅ (À créer)
- [ ] DisputeService.java
- [ ] DisputeAuthorizationService.java
- [ ] DisputeResolutionService.java

### 5. CONTROLLERS ✅ (À créer)
- [ ] DisputeController.java

### 6. REPOSITORIES ✅ (À créer)
- [ ] DisputeRepository.java
- [ ] DisputeEvidenceRepository.java

### 7. KAFKA EVENTS ✅ (À créer)
- [ ] DisputeCreatedEvent.java
- [ ] DisputeResolvedEvent.java

### 8. DATABASE ✅ (À créer)
- [ ] V20251216_3__create_dispute_tables.sql

---

## 📊 IMPLEMENTATION PLAN

**Total Code Lines Expected:** 2,000+
**Total Files to Create:** 18
**Endpoints:** 6
**Services:** 3
**Test Cases:** 12+

---

## ✨ COHERENCE AVEC PHASE 1 & 2

| Aspect | Phase 1 | Phase 2 | Phase 3 |
|--------|---------|---------|---------|
| **Architecture** | Layered | Layered | ✅ Layered |
| **JWT Extraction** | Real | Real | ✅ Real (same pattern) |
| **Authorization** | @PreAuthorize | Multi-level | ✅ Multi-level (3+ checks) |
| **Error Handling** | 5 types | 5 types | ✅ 7 types |
| **Audit Logging** | Complete | Complete | ✅ Complete (13+ fields) |
| **Kafka Events** | Published | Published | ✅ Published (2 new topics) |
| **Type Safety** | 100% | 100% | ✅ 100% |
| **Compilation** | 0 errors | 0 errors | ✅ 0 errors (expected) |

---

## 🔧 IMPLÉMENTATION DÉTAILLÉE

### PHASE 3A: Models & Enums (Jour 1 - matin)

#### 1. DisputeStatus Enum
```java
// Path: transaction-service/src/main/java/com/zaphira/transaction/model/enums/DisputeStatus.java
public enum DisputeStatus {
    INITIATED,           // Dispute créé, en attente de validation
    UNDER_INVESTIGATION, // En investigation par notre équipe
    AWAITING_EVIDENCE,   // En attente d'évidence du client
    AWAITING_RESPONSE,   // En attente de réponse du défendant
    RESOLVED,           // Résolution finalisée
    CLOSED,             // Cas fermé
    APPEAL_REQUESTED,   // Appel demandé
    ESCALATED,          // Escaladé à management
    EXPIRED             // Délai d'action dépassé
}
```

#### 2. DisputeCategory Enum
```java
// Path: transaction-service/src/main/java/com/zaphira/transaction/model/enums/DisputeCategory.java
public enum DisputeCategory {
    // Customer claims
    TRANSACTION_NOT_RECOGNIZED,
    FRAUDULENT_TRANSACTION,
    DUPLICATE_CHARGE,
    INCORRECT_AMOUNT,
    SERVICE_NOT_PROVIDED,
    SERVICE_QUALITY_ISSUE,
    
    // Merchant claims
    PAYMENT_NOT_RECEIVED,
    PARTIAL_PAYMENT,
    NON_DELIVERY,
    CUSTOMER_NO_SHOW,
    CHARGEBACK_THREAT,
    
    // Operational
    TECHNICAL_ERROR,
    SYSTEM_MALFUNCTION,
    PROCESSING_ERROR,
    TIMEOUT_ERROR,
    
    // Other
    OTHER
}
```

#### 3. DisputeResolutionType Enum
```java
// Path: transaction-service/src/main/java/com/zaphira/transaction/model/enums/DisputeResolutionType.java
public enum DisputeResolutionType {
    APPROVED,           // Dispute approuvé, refund customer
    DENIED,            // Dispute rejeté, refund merchant
    PARTIAL_APPROVAL,   // Remboursement partiel
    SETTLEMENT,        // Accord entre les parties
    WITHDRAWN,         // Retiré par le client
    EXPIRED,           // Expiré sans action
    ESCALATED_TO_BANK  // Escaladé à la banque
}
```

#### 4. EvidenceType Enum
```java
// Path: transaction-service/src/main/java/com/zaphira/transaction/model/enums/EvidenceType.java
public enum EvidenceType {
    RECEIPT,           // Reçu de transaction
    INVOICE,           // Facture
    DELIVERY_PROOF,    // Preuve de livraison
    COMMUNICATION,     // Preuves de communication (email, chat)
    CONTRACT,          // Contrat ou accord
    BANK_STATEMENT,    // Relevé bancaire
    SCREENSHOT,        // Capture d'écran
    VIDEO_RECORDING,   // Enregistrement vidéo
    AUDIO_RECORDING,   // Enregistrement audio
    DOCUMENT,          // Autre document
    OTHER              // Autre
}
```

#### 5. Dispute Model Entity
```java
// Path: transaction-service/src/main/java/com/zaphira/transaction/model/Dispute.java
@Entity
@Table(name = "disputes", indexes = {
    @Index(name = "idx_disputes_transaction", columnList = "transaction_id"),
    @Index(name = "idx_disputes_status", columnList = "status"),
    @Index(name = "idx_disputes_created_at", columnList = "created_at DESC"),
    @Index(name = "idx_disputes_initiator", columnList = "initiated_by")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Dispute {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Transaction Link
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", insertable = false, updatable = false)
    private Transaction transaction;
    
    // Dispute Details
    @Column(name = "reference", unique = true, length = 50)
    private String reference; // "DSP-" + UUID
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private DisputeCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DisputeStatus status;
    
    // Dispute Information
    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "claimed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal claimedAmount;
    
    @Column(name = "claimed_currency", length = 3)
    private String claimedCurrency;
    
    // Party Information
    @Column(name = "initiated_by", nullable = false, length = 255) // Email/User ID
    private String initiatedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "initiator_role", nullable = false)
    private DisputeInitiatorRole initiatorRole; // CUSTOMER, MERCHANT, ADMIN
    
    @Column(name = "initiated_by_wallet", length = 50)
    private String initiatedByWallet;
    
    // Resolution
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type")
    private DisputeResolutionType resolutionType;
    
    @Column(name = "resolved_by", length = 255)
    private String resolvedBy; // ADMIN email/ID
    
    @Column(name = "resolution_reason", columnDefinition = "TEXT")
    private String resolutionReason;
    
    @Column(name = "resolution_amount", precision = 19, scale = 4)
    private BigDecimal resolutionAmount;
    
    // Timeline
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    // Status Tracking
    @Column(name = "evidence_submitted_count")
    private Integer evidenceSubmittedCount;
    
    @Column(name = "last_evidence_submitted_at")
    private LocalDateTime lastEvidenceSubmittedAt;
    
    @Column(name = "appeal_count")
    private Integer appealCount;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata; // JSON: additional context
    
    // Relations
    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<DisputeEvidence> evidences;
    
    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<DisputeTimeline> timeline;
    
    // Audit
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "request_id", length = 100)
    private String requestId; // Distributed tracing
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = DisputeStatus.INITIATED;
        this.deadlineAt = LocalDateTime.now().plusDays(30); // 30 days deadline
        this.evidenceSubmittedCount = 0;
        this.appealCount = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

#### 6. DisputeEvidence Model
```java
// Path: transaction-service/src/main/java/com/zaphira/transaction/model/DisputeEvidence.java
@Entity
@Table(name = "dispute_evidence", indexes = {
    @Index(name = "idx_evidence_dispute", columnList = "dispute_id"),
    @Index(name = "idx_evidence_type", columnList = "evidence_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeEvidence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false)
    private EvidenceType evidenceType;
    
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl; // S3, Cloud Storage URL
    
    @Column(name = "file_name", length = 255)
    private String fileName;
    
    @Column(name = "file_size")
    private Long fileSize; // bytes
    
    @Column(name = "mime_type", length = 100)
    private String mimeType; // image/png, application/pdf, etc
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "submitted_by", nullable = false, length = 255)
    private String submittedBy;
    
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;
    
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;
    
    @Column(name = "verified_by", length = 255)
    private String verifiedBy;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }
}
```

#### 7. DisputeTimeline Model
```java
// Path: transaction-service/src/main/java/com/zaphira/transaction/model/DisputeTimeline.java
@Entity
@Table(name = "dispute_timeline", indexes = {
    @Index(name = "idx_timeline_dispute", columnList = "dispute_id"),
    @Index(name = "idx_timeline_timestamp", columnList = "event_timestamp DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeTimeline {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // CREATED, EVIDENCE_ADDED, STATUS_CHANGED, RESOLVED, etc
    
    @Column(name = "event_description", columnDefinition = "TEXT")
    private String eventDescription;
    
    @Column(name = "actor", nullable = false, length = 255)
    private String actor; // Who performed action
    
    @Column(name = "actor_role", length = 50)
    private String actorRole; // CUSTOMER, MERCHANT, ADMIN, SYSTEM
    
    @Column(name = "old_status", length = 50)
    private String oldStatus;
    
    @Column(name = "new_status", length = 50)
    private String newStatus;
    
    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private LocalDateTime eventTimestamp;
    
    @PrePersist
    protected void onCreate() {
        this.eventTimestamp = LocalDateTime.now();
    }
}
```

#### 8. DisputeInitiatorRole Enum
```java
// Path: transaction-service/src/main/java/com/zaphira/transaction/model/enums/DisputeInitiatorRole.java
public enum DisputeInitiatorRole {
    CUSTOMER,          // Customer raised dispute
    MERCHANT,          // Merchant raised dispute
    ADMIN,            // Admin/Support raised dispute
    SYSTEM            // System auto-escalated
}
```

---

**Continuation Files (Ready to Implement):**
- Services (3 files)
- DTOs (4 files)
- Controller (1 file)
- Repositories (2 files)
- Kafka Events (2 files)
- Database Migration (1 file)

**Status:** Models & Enums ready for creation ✅

