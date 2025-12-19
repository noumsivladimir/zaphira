# PLAN D'IMPLÉMENTATION DÉTAILLÉ - TRANSACTION-SERVICE
## Roadmap Complète avec Ordre Optimal

**Date:** 16 Décembre 2025
**Version:** 1.0
**Cible:** Complétion 75% → 100% de couverture fonctionnelle

---

# PHASE 1: FONDATIONS CRITIQUES (Priorité: IMMÉDIATE)
## Semaine 1-2 | Impact: Très Haut | Effort: Moyen

### 1.1 Advanced Search & Filtering [PRIORITÉ 1]

**Statut Actuel:** ⏳ 40% (seulement GET all)

**À Implémenter:**

#### 1.1.1 Search Service
**Classe:** `TransactionSearchService.java`

```java
public class TransactionSearchService {
  // Méthodes à créer:
  
  // 1. Search avec filtres
  Page<Transaction> search(TransactionSearchRequest request, Pageable pageable);
  
  // 2. Search request DTO
  class TransactionSearchRequest {
    private Long senderWalletId;
    private Long receiverWalletId;
    private BigDecimal amountMin;
    private BigDecimal amountMax;
    private TransactionStatus status;
    private TransactionType type;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private String reference;
    private String currency;
  }
}
```

#### 1.1.2 Repository Queries
**Classe:** `TransactionRepository.java` (extensions)

```java
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  // Nouvelles méthodes:
  Page<Transaction> findBySenderWalletNumberAndStatus(String senderWalletNumber, TransactionStatus status, Pageable page);
  Page<Transaction> findByAmountBetween(BigDecimal min, BigDecimal max, Pageable page);
  Page<Transaction> findByStatusAndCreatedAtBetween(TransactionStatus status, LocalDateTime from, LocalDateTime to, Pageable page);
  Page<Transaction> findByTypeAndCurrency(TransactionType type, String currency, Pageable page);
  
  // Custom query
  @Query("SELECT t FROM Transaction t WHERE " +
         "(:senderWallet IS NULL OR t.senderWalletNumber = :senderWallet) AND " +
         "(:status IS NULL OR t.status = :status) AND " +
         "(:amountMin IS NULL OR t.amount >= :amountMin) AND " +
         "(:amountMax IS NULL OR t.amount <= :amountMax) AND " +
         "(:from IS NULL OR t.createdAt >= :from) AND " +
         "(:to IS NULL OR t.createdAt <= :to)")
  Page<Transaction> searchTransactions(
    @Param("senderWallet") String senderWallet,
    @Param("status") TransactionStatus status,
    @Param("amountMin") BigDecimal amountMin,
    @Param("amountMax") BigDecimal amountMax,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to,
    Pageable pageable
  );
}
```

#### 1.1.3 Endpoints REST

**TransactionController.java** - Ajouter:

```java
@GetMapping("/search")
public ResponseEntity<Page<Transaction>> searchTransactions(
  @RequestParam(required = false) String senderWallet,
  @RequestParam(required = false) TransactionStatus status,
  @RequestParam(required = false) BigDecimal amountMin,
  @RequestParam(required = false) BigDecimal amountMax,
  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
  @RequestParam(defaultValue = "0") int page,
  @RequestParam(defaultValue = "20") int size,
  @RequestParam(defaultValue = "createdAt") String sortBy,
  @RequestParam(defaultValue = "DESC") Sort.Direction direction
) {
  Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
  TransactionSearchRequest searchRequest = TransactionSearchRequest.builder()
    .senderWalletNumber(senderWallet)
    .status(status)
    .amountMin(amountMin)
    .amountMax(amountMax)
    .createdFrom(fromDate)
    .createdTo(toDate)
    .build();
  
  Page<Transaction> results = transactionSearchService.search(searchRequest, pageable);
  return ResponseEntity.ok(results);
}

@GetMapping("/export")
public ResponseEntity<Resource> exportTransactions(
  @RequestParam(required = false) String format, // "CSV", "JSON"
  @RequestParam(required = false) TransactionStatus status
) throws IOException {
  // Implementation: CSV/JSON export
  Resource resource = transactionSearchService.exportTransactions(status, format);
  return ResponseEntity.ok()
    .contentType(MediaType.APPLICATION_OCTET_STREAM)
    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions." + format.toLowerCase())
    .body(resource);
}
```

#### 1.1.4 Database Indexes

**Migration:** `V20251216__add_search_indexes.sql`

```sql
-- Index pour recherche par wallet
CREATE INDEX idx_transactions_sender_wallet ON transactions(sender_wallet_number);
CREATE INDEX idx_transactions_receiver_wallet ON transactions(receiver_wallet_number);

-- Index pour recherche par montant et date
CREATE INDEX idx_transactions_amount_created ON transactions(amount, created_at);

-- Index composite pour filtres courants
CREATE INDEX idx_transactions_status_created ON transactions(status, created_at DESC);
CREATE INDEX idx_transactions_type_currency ON transactions(type, currency);

-- Index pour recherche par period
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);
```

#### 1.1.5 DTO

```java
@Data
@Builder
public class TransactionSearchRequest {
  private String senderWalletNumber;
  private String receiverWalletNumber;
  private BigDecimal amountMin;
  private BigDecimal amountMax;
  private TransactionStatus status;
  private TransactionType type;
  private LocalDateTime createdFrom;
  private LocalDateTime createdTo;
  private String reference;
  private String currency;
}

@Data
@AllArgsConstructor
public class TransactionExportDTO {
  private Long id;
  private String reference;
  private String senderWallet;
  private String receiverWallet;
  private BigDecimal amount;
  private String currency;
  private BigDecimal feeAmount;
  private TransactionStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
}
```

**Effort:** 3-4 jours | **Dépendances:** Aucune | **Impact:** 🔴 Critique (utilisateurs attendent)

---

### 1.2 Reversal & Refund Services [PRIORITÉ 2]

**Statut Actuel:** ❌ Complètement manquant (sauf status update)

**À Implémenter:**

#### 1.2.1 Reversal Service

**Classe:** `TransactionReversalService.java`

```java
@Service
@Transactional
public class TransactionReversalService {
  
  // Reverse transaction complet
  public Transaction reverseTransaction(
    Long transactionId, 
    String reason, 
    String requestedBy
  ) {
    // 1. Validate transaction can be reversed
    Transaction original = transactionRepository.findById(transactionId)
      .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    
    if (!isReversalEligible(original)) {
      throw new ReversalNotAllowedException(
        "Transaction " + transactionId + " cannot be reversed at status " + original.getStatus()
      );
    }
    
    // 2. Create reverse transaction (matching entry, opposite amounts)
    Transaction reversal = Transaction.builder()
      .reference("REV-" + UUID.randomUUID())
      .senderWalletNumber(original.getReceiverWalletNumber()) // Reversed
      .receiverWalletNumber(original.getSenderWalletNumber())
      .amount(original.getAmount())
      .currency(original.getCurrency())
      .type(TransactionType.REVERSAL)
      .status(TransactionStatus.INITIATED)
      .relatedTransactionId(transactionId)
      .reversalReason(reason)
      .build();
    
    // 3. Process reversal (refund wallet immediately)
    WalletTransferRequest reverseTransfer = WalletTransferRequest.builder()
      .fromWallet(original.getReceiverWalletNumber())
      .toWallet(original.getSenderWalletNumber())
      .amount(original.getAmount())
      .currency(original.getCurrency())
      .type("REVERSAL")
      .transactionRef(reversal.getReference())
      .build();
    
    try {
      feignWalletClient.transfer(reverseTransfer);
      reversal.applyStatus(TransactionStatus.COMPLETED);
    } catch (Exception e) {
      reversal.applyStatus(TransactionStatus.FAILED);
      throw new WalletOperationException("Reversal transfer failed", e);
    }
    
    // 4. Update original transaction
    original.applyStatus(TransactionStatus.REVERSED);
    original.setReversalId(reversal.getId());
    original.setReversedAt(LocalDateTime.now());
    
    // 5. Record audit
    recordState(original, TransactionStatus.REVERSED, requestedBy, "Reversal: " + reason);
    recordState(reversal, TransactionStatus.COMPLETED, requestedBy, "Reversal of txn " + transactionId);
    
    // 6. Publish events
    transactionEventPublisher.publishTransactionReversed(
      TransactionReversedEvent.builder()
        .originalTransactionId(original.getId())
        .reversalTransactionId(reversal.getId())
        .reason(reason)
        .createdAt(LocalDateTime.now())
        .build()
    );
    
    transactionRepository.save(original);
    return transactionRepository.save(reversal);
  }
  
  // Helper: Check if transaction eligible for reversal
  private boolean isReversalEligible(Transaction transaction) {
    Set<TransactionStatus> reversibleStatuses = Set.of(
      TransactionStatus.COMPLETED,
      TransactionStatus.AUTHORIZED,
      TransactionStatus.PROCESSING
    );
    return reversibleStatuses.contains(transaction.getStatus());
  }
}
```

#### 1.2.2 Refund Service

**Classe:** `TransactionRefundService.java`

```java
@Service
@Transactional
public class TransactionRefundService {
  
  // Full refund (reversal + fee return)
  public Transaction refundTransaction(
    Long transactionId,
    String reason,
    String requestedBy
  ) {
    Transaction original = transactionRepository.findById(transactionId)
      .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    
    if (!isRefundEligible(original)) {
      throw new RefundNotAllowedException(
        "Transaction cannot be refunded at status " + original.getStatus()
      );
    }
    
    // 1. Create refund transaction
    Transaction refund = Transaction.builder()
      .reference("RFD-" + UUID.randomUUID())
      .senderWalletNumber(original.getReceiverWalletNumber())
      .receiverWalletNumber(original.getSenderWalletNumber())
      .amount(original.getAmount())
      .currency(original.getCurrency())
      .feeAmount(original.getFeeAmount()) // Also refund fee
      .type(TransactionType.REFUND)
      .status(TransactionStatus.INITIATED)
      .relatedTransactionId(transactionId)
      .refundReason(reason)
      .build();
    
    // 2. Refund principal amount
    WalletTransferRequest principalRefund = WalletTransferRequest.builder()
      .fromWallet(original.getReceiverWalletNumber())
      .toWallet(original.getSenderWalletNumber())
      .amount(original.getAmount())
      .type("REFUND_PRINCIPAL")
      .transactionRef(refund.getReference())
      .build();
    
    // 3. Refund fees (return to fee account or customer)
    WalletTransferRequest feeRefund = null;
    if (original.getFeeAmount() != null && original.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
      feeRefund = WalletTransferRequest.builder()
        .fromWallet("FEE_ACCOUNT") // Central fee account
        .toWallet(original.getSenderWalletNumber())
        .amount(original.getFeeAmount())
        .type("REFUND_FEE")
        .transactionRef(refund.getReference())
        .build();
    }
    
    try {
      // Execute transfers
      feignWalletClient.transfer(principalRefund);
      if (feeRefund != null) {
        feignWalletClient.transfer(feeRefund);
      }
      refund.applyStatus(TransactionStatus.COMPLETED);
    } catch (Exception e) {
      refund.applyStatus(TransactionStatus.FAILED);
      throw new WalletOperationException("Refund failed", e);
    }
    
    // 4. Update original
    original.applyStatus(TransactionStatus.REFUNDED);
    original.setRefundId(refund.getId());
    original.setRefundedAt(LocalDateTime.now());
    
    // 5. Audit & Events
    recordState(original, TransactionStatus.REFUNDED, requestedBy, "Refund: " + reason);
    recordState(refund, TransactionStatus.COMPLETED, requestedBy, "Refund of txn " + transactionId);
    
    transactionEventPublisher.publishTransactionRefunded(
      TransactionRefundedEvent.builder()
        .originalTransactionId(original.getId())
        .refundTransactionId(refund.getId())
        .principalAmount(original.getAmount())
        .feeAmount(original.getFeeAmount())
        .reason(reason)
        .createdAt(LocalDateTime.now())
        .build()
    );
    
    return transactionRepository.save(refund);
  }
  
  // Partial refund
  public Transaction partialRefund(
    Long transactionId,
    BigDecimal refundAmount,
    String reason,
    String requestedBy
  ) {
    Transaction original = transactionRepository.findById(transactionId)
      .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    
    if (refundAmount.compareTo(original.getAmount()) > 0) {
      throw new InvalidRefundAmountException(
        "Refund amount cannot exceed transaction amount"
      );
    }
    
    // Similar logic as full refund, but with partial amount
    // ...implementation details...
    
    return null;
  }
  
  private boolean isRefundEligible(Transaction transaction) {
    return transaction.getStatus() == TransactionStatus.COMPLETED;
  }
}
```

#### 1.2.3 Endpoints

```java
@PostMapping("/{id}/reverse")
public ResponseEntity<Transaction> reverseTransaction(
  @PathVariable Long id,
  @RequestBody ReverseTransactionRequest request,
  @RequestHeader String userId
) {
  Transaction reversed = transactionReversalService.reverseTransaction(
    id,
    request.getReason(),
    userId
  );
  return ResponseEntity.ok(reversed);
}

@PostMapping("/{id}/refund")
public ResponseEntity<Transaction> refundTransaction(
  @PathVariable Long id,
  @RequestBody RefundTransactionRequest request,
  @RequestHeader String userId
) {
  if (request.isPartial()) {
    return ResponseEntity.ok(
      transactionRefundService.partialRefund(
        id,
        request.getAmount(),
        request.getReason(),
        userId
      )
    );
  }
  return ResponseEntity.ok(
    transactionRefundService.refundTransaction(
      id,
      request.getReason(),
      userId
    )
  );
}
```

#### 1.2.4 Database Changes

```sql
-- Transaction table additions
ALTER TABLE transactions ADD COLUMN related_transaction_id BIGINT;
ALTER TABLE transactions ADD COLUMN reversal_reason VARCHAR(255);
ALTER TABLE transactions ADD COLUMN reversal_id BIGINT;
ALTER TABLE transactions ADD COLUMN reversed_at TIMESTAMP;
ALTER TABLE transactions ADD COLUMN refund_reason VARCHAR(255);
ALTER TABLE transactions ADD COLUMN refund_id BIGINT;
ALTER TABLE transactions ADD COLUMN refunded_at TIMESTAMP;

-- Add indexes
CREATE INDEX idx_transactions_related ON transactions(related_transaction_id);
CREATE INDEX idx_transactions_reversal ON transactions(reversal_id);
CREATE INDEX idx_transactions_refund ON transactions(refund_id);
```

#### 1.2.5 New Enums

```java
public enum TransactionType {
  TRANSFER,
  PAYMENT,
  TOPUP,
  WITHDRAWAL,
  SCHEDULED,
  REVERSAL,      // NEW
  REFUND,        // NEW
  DISPUTE_RESOLUTION // NEW
}
```

#### 1.2.6 Events

```java
@Data
@Builder
public class TransactionReversedEvent {
  private Long originalTransactionId;
  private Long reversalTransactionId;
  private String reason;
  private LocalDateTime createdAt;
}

@Data
@Builder
public class TransactionRefundedEvent {
  private Long originalTransactionId;
  private Long refundTransactionId;
  private BigDecimal principalAmount;
  private BigDecimal feeAmount;
  private String reason;
  private LocalDateTime createdAt;
}
```

**Effort:** 5-6 jours | **Dépendances:** Wallet Service | **Impact:** 🔴 Critique (client requests)

---

# PHASE 2: GESTION AVANCÉE (Priorité: HAUTE)
## Semaine 3-4 | Impact: Élevé | Effort: Élevé

### 2.1 Dispute Management [PRIORITÉ 3]

**Statut Actuel:** ❌ Complètement manquant

**À Implémenter:**

#### 2.1.1 Dispute Entity

```java
@Entity
@Table(name = "disputes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Dispute {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @Column(nullable = false, unique = true)
  private String disputeReference;
  
  @Column(nullable = false)
  private Long transactionId;
  
  @Column(nullable = false)
  private Long openedBy; // User ID
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DisputeStatus status; // OPENED, UNDER_INVESTIGATION, RESOLVED, CLOSED, REJECTED
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DisputeReason reason; // UNAUTHORIZED, DUPLICATE, MERCHANT_ERROR, NOT_RECEIVED, QUALITY_ISSUE, OTHER
  
  @Column(columnDefinition = "TEXT")
  private String description;
  
  @Column(nullable = false)
  private LocalDateTime openedAt;
  
  @Column
  private LocalDateTime investigatedAt;
  
  @Column
  private LocalDateTime resolvedAt;
  
  @Column(nullable = false)
  private LocalDateTime dueDate; // Deadline for response
  
  @Column(columnDefinition = "TEXT")
  private String resolutionNotes;
  
  @Enumerated(EnumType.STRING)
  private DisputeResolution resolution; // CHARGEBACK, REFUND, REJECT, PARTIAL_REFUND
  
  @Column(precision = 19, scale = 4)
  private BigDecimal refundAmount;
  
  @Column
  private Long resolvedBy; // User ID
  
  @Column
  private LocalDateTime closedAt;
}

public enum DisputeStatus {
  OPENED,
  UNDER_INVESTIGATION,
  EVIDENCE_REQUESTED,
  AWAITING_MERCHANT_RESPONSE,
  RESOLVED,
  CLOSED,
  REJECTED
}

public enum DisputeReason {
  UNAUTHORIZED("Unauthorized transaction"),
  DUPLICATE("Duplicate charge"),
  MERCHANT_ERROR("Merchant error"),
  NOT_RECEIVED("Funds not received"),
  QUALITY_ISSUE("Quality/Service issue"),
  AMOUNT_MISMATCH("Wrong amount charged"),
  OTHER("Other reason");
  
  private String description;
}

public enum DisputeResolution {
  CHARGEBACK,      // Full refund, merchant liable
  REFUND,          // Full refund as goodwill
  PARTIAL_REFUND,  // Partial refund
  REJECT,          // Dispute rejected
  COMPROMISE       // Shared settlement
}
```

#### 2.1.2 Dispute Evidence Entity

```java
@Entity
@Table(name = "dispute_evidence")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisputeEvidence {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @Column(nullable = false)
  private Long disputeId;
  
  @Column(nullable = false)
  private String evidenceType; // "RECEIPT", "COMMUNICATION", "PROOF_OF_DELIVERY", "INVOICE", "SCREENSHOT"
  
  @Column
  private String fileUrl; // S3 path
  
  @Column(nullable = false)
  private String fileName;
  
  @Column
  private String contentType;
  
  @Column(nullable = false)
  private LocalDateTime uploadedAt;
  
  @Column(nullable = false)
  private Long uploadedBy; // User ID
  
  @Column(columnDefinition = "TEXT")
  private String description;
  
  @Column
  private String status; // PENDING_REVIEW, VERIFIED, REJECTED
}
```

#### 2.1.3 Dispute Service

```java
@Service
@Transactional
public class DisputeService {
  
  // Create dispute
  public Dispute createDispute(DisputeCreateRequest request, Long userId) {
    // 1. Validate transaction exists
    Transaction transaction = transactionRepository.findById(request.getTransactionId())
      .orElseThrow(() -> new TransactionNotFoundException(request.getTransactionId()));
    
    // 2. Check dispute not already exists
    if (disputeRepository.existsByTransactionId(request.getTransactionId())) {
      throw new DisputeAlreadyExistsException(
        "Dispute already exists for transaction " + request.getTransactionId()
      );
    }
    
    // 3. Check transaction is disputable (not too old - configurable, default 180 days)
    LocalDateTime deadline = transaction.getCompletedAt().plusDays(disputeProperties.getDisputeWindow());
    if (LocalDateTime.now().isAfter(deadline)) {
      throw new DisputeWindowClosedException(
        "Dispute window has closed for this transaction"
      );
    }
    
    // 4. Create dispute
    Dispute dispute = Dispute.builder()
      .disputeReference("DSP-" + UUID.randomUUID())
      .transactionId(request.getTransactionId())
      .openedBy(userId)
      .status(DisputeStatus.OPENED)
      .reason(request.getReason())
      .description(request.getDescription())
      .openedAt(LocalDateTime.now())
      .dueDate(LocalDateTime.now().plusDays(disputeProperties.getInvestigationDays()))
      .build();
    
    dispute = disputeRepository.save(dispute);
    
    // 5. Update transaction status
    transaction.applyStatus(TransactionStatus.UNDER_DISPUTE);
    transactionRepository.save(transaction);
    
    // 6. Publish event
    transactionEventPublisher.publishDisputeOpened(
      DisputeOpenedEvent.builder()
        .disputeId(dispute.getId())
        .transactionId(transaction.getId())
        .reason(request.getReason())
        .openedAt(LocalDateTime.now())
        .build()
    );
    
    // 7. Send notification to merchant
    notificationService.sendMerchantNotification(
      "DISPUTE_OPENED",
      transaction.getReceiverWalletNumber(),
      dispute
    );
    
    return dispute;
  }
  
  // Add evidence
  public DisputeEvidence addEvidence(
    Long disputeId,
    MultipartFile file,
    String evidenceType,
    String description,
    Long userId
  ) throws IOException {
    Dispute dispute = disputeRepository.findById(disputeId)
      .orElseThrow(() -> new DisputeNotFoundException(disputeId));
    
    // 1. Upload file to S3
    String fileUrl = fileStorageService.uploadFile(
      file,
      "disputes/" + dispute.getDisputeReference() + "/"
    );
    
    // 2. Create evidence record
    DisputeEvidence evidence = DisputeEvidence.builder()
      .disputeId(disputeId)
      .evidenceType(evidenceType)
      .fileUrl(fileUrl)
      .fileName(file.getOriginalFilename())
      .contentType(file.getContentType())
      .uploadedAt(LocalDateTime.now())
      .uploadedBy(userId)
      .description(description)
      .status("PENDING_REVIEW")
      .build();
    
    return disputeEvidenceRepository.save(evidence);
  }
  
  // Investigate dispute
  public Dispute investigateDispute(Long disputeId, String investigationNotes) {
    Dispute dispute = disputeRepository.findById(disputeId)
      .orElseThrow(() -> new DisputeNotFoundException(disputeId));
    
    dispute.setStatus(DisputeStatus.UNDER_INVESTIGATION);
    dispute.setInvestigatedAt(LocalDateTime.now());
    
    return disputeRepository.save(dispute);
  }
  
  // Resolve dispute
  public Dispute resolveDispute(
    Long disputeId,
    DisputeResolution resolution,
    BigDecimal refundAmount,
    String notes,
    Long resolvedBy
  ) {
    Dispute dispute = disputeRepository.findById(disputeId)
      .orElseThrow(() -> new DisputeNotFoundException(disputeId));
    
    // 1. Process resolution
    Transaction originalTransaction = transactionRepository.findById(dispute.getTransactionId())
      .orElseThrow();
    
    if (resolution == DisputeResolution.CHARGEBACK || resolution == DisputeResolution.REFUND) {
      // Create refund transaction
      transactionRefundService.partialRefund(
        dispute.getTransactionId(),
        refundAmount,
        "Dispute Resolution: " + resolution,
        "SYSTEM"
      );
    }
    
    // 2. Update dispute
    dispute.setStatus(DisputeStatus.RESOLVED);
    dispute.setResolution(resolution);
    dispute.setRefundAmount(refundAmount);
    dispute.setResolutionNotes(notes);
    dispute.setResolvedBy(resolvedBy);
    dispute.setResolvedAt(LocalDateTime.now());
    
    dispute = disputeRepository.save(dispute);
    
    // 3. Update transaction
    originalTransaction.applyStatus(TransactionStatus.DISPUTE_RESOLVED);
    transactionRepository.save(originalTransaction);
    
    // 4. Publish event
    transactionEventPublisher.publishDisputeResolved(
      DisputeResolvedEvent.builder()
        .disputeId(dispute.getId())
        .resolution(resolution)
        .refundAmount(refundAmount)
        .resolvedAt(LocalDateTime.now())
        .build()
    );
    
    return dispute;
  }
  
  // List disputes
  public Page<Dispute> listDisputes(DisputeSearchCriteria criteria, Pageable pageable) {
    return disputeRepository.searchDisputes(criteria, pageable);
  }
}
```

#### 2.1.4 Endpoints

```java
@RestController
@RequestMapping("/api/disputes")
public class DisputeController {
  
  @PostMapping
  public ResponseEntity<Dispute> createDispute(
    @RequestBody DisputeCreateRequest request,
    @RequestHeader Long userId
  ) {
    Dispute dispute = disputeService.createDispute(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(dispute);
  }
  
  @GetMapping("/{id}")
  public ResponseEntity<DisputeDetailResponse> getDispute(@PathVariable Long id) {
    Dispute dispute = disputeService.getDispute(id);
    List<DisputeEvidence> evidence = disputeEvidenceRepository.findByDisputeId(id);
    return ResponseEntity.ok(
      DisputeDetailResponse.builder()
        .dispute(dispute)
        .evidence(evidence)
        .build()
    );
  }
  
  @PostMapping("/{id}/evidence")
  public ResponseEntity<DisputeEvidence> uploadEvidence(
    @PathVariable Long id,
    @RequestParam MultipartFile file,
    @RequestParam String evidenceType,
    @RequestParam(required = false) String description,
    @RequestHeader Long userId
  ) throws IOException {
    DisputeEvidence evidence = disputeService.addEvidence(
      id, file, evidenceType, description, userId
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(evidence);
  }
  
  @PutMapping("/{id}/investigate")
  public ResponseEntity<Dispute> investigateDispute(
    @PathVariable Long id,
    @RequestBody InvestigateDisputeRequest request
  ) {
    Dispute dispute = disputeService.investigateDispute(id, request.getNotes());
    return ResponseEntity.ok(dispute);
  }
  
  @PutMapping("/{id}/resolve")
  public ResponseEntity<Dispute> resolveDispute(
    @PathVariable Long id,
    @RequestBody ResolveDisputeRequest request,
    @RequestHeader Long userId
  ) {
    Dispute dispute = disputeService.resolveDispute(
      id,
      request.getResolution(),
      request.getRefundAmount(),
      request.getNotes(),
      userId
    );
    return ResponseEntity.ok(dispute);
  }
  
  @GetMapping
  public ResponseEntity<Page<Dispute>> listDisputes(
    @RequestParam(required = false) DisputeStatus status,
    @RequestParam(required = false) String reason,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    DisputeSearchCriteria criteria = DisputeSearchCriteria.builder()
      .status(status)
      .reason(reason)
      .build();
    
    Pageable pageable = PageRequest.of(page, size, Sort.by("openedAt").descending());
    Page<Dispute> disputes = disputeService.listDisputes(criteria, pageable);
    return ResponseEntity.ok(disputes);
  }
}
```

**Effort:** 8-10 jours | **Dépendances:** Refund Service, File Storage | **Impact:** 🔴 Critique

---

### 2.2 Multi-Currency & Exchange Rates [PRIORITÉ 4]

**Statut Actuel:** ⏳ Partiellement (champ currency, pas d'exchange)

**À Implémenter:**

#### 2.2.1 Exchange Rate Service

```java
@Service
public class ExchangeRateService {
  
  // Cache exchange rates (Redis)
  @Cacheable(value = "exchangeRates", key = "#from + ':' + #to")
  public BigDecimal getExchangeRate(String from, String to) {
    if (from.equals(to)) return BigDecimal.ONE;
    
    // Call external provider (OpenExchangeRates, XE, etc.)
    ExchangeRateResponse response = exchangeRateClient.getRate(from, to);
    return response.getRate();
  }
  
  // Convert amount
  public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
    BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
    return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
  }
  
  // Settlement currency conversion
  public TransactionSettlement getSettlementAmount(Transaction transaction) {
    String settlementCurrency = configProperties.getSettlementCurrency(); // "USD"
    
    BigDecimal principalInSettlement = convert(
      transaction.getAmount(),
      transaction.getCurrency(),
      settlementCurrency
    );
    
    BigDecimal feeInSettlement = transaction.getFeeAmount() != null
      ? convert(transaction.getFeeAmount(), transaction.getFeeCurrency(), settlementCurrency)
      : BigDecimal.ZERO;
    
    BigDecimal fxFeeAmount = principalInSettlement.subtract(transaction.getAmount());
    
    return TransactionSettlement.builder()
      .transactionId(transaction.getId())
      .originalAmount(transaction.getAmount())
      .originalCurrency(transaction.getCurrency())
      .settlementAmount(principalInSettlement)
      .settlementCurrency(settlementCurrency)
      .exchangeRate(getExchangeRate(transaction.getCurrency(), settlementCurrency))
      .fxFeeAmount(fxFeeAmount)
      .totalSettlementAmount(principalInSettlement.add(feeInSettlement).add(fxFeeAmount))
      .build();
  }
}
```

#### 2.2.2 Multi-Currency Fee Calculation

```java
@Service
public class MultiCurrencyFeeService {
  
  public FeeCalculationResult calculateFeeWithFX(TransactionRequest request) {
    // 1. Calculate base fee in transaction currency
    BigDecimal baseFee = calculateBaseFee(request.getAmount());
    
    // 2. Add FX fee if converting
    BigDecimal fxFee = BigDecimal.ZERO;
    if (!request.getCurrency().equals(configProperties.getSettlementCurrency())) {
      BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
        request.getCurrency(),
        configProperties.getSettlementCurrency()
      );
      fxFee = request.getAmount()
        .multiply(exchangeRate)
        .subtract(request.getAmount())
        .abs()
        .multiply(feeProperties.getFxFeePercentage());
    }
    
    BigDecimal totalFee = baseFee.add(fxFee);
    
    return FeeCalculationResult.builder()
      .transactionFee(baseFee)
      .fxFee(fxFee)
      .totalFee(totalFee)
      .currency(request.getCurrency())
      .build();
  }
}
```

#### 2.2.3 Settlement Table

```sql
CREATE TABLE transaction_settlements (
  id BIGSERIAL PRIMARY KEY,
  transaction_id BIGINT NOT NULL UNIQUE,
  original_amount NUMERIC(19,4) NOT NULL,
  original_currency VARCHAR(3) NOT NULL,
  settlement_amount NUMERIC(19,4) NOT NULL,
  settlement_currency VARCHAR(3) NOT NULL,
  exchange_rate NUMERIC(19,6) NOT NULL,
  fx_fee NUMERIC(19,4),
  total_settlement NUMERIC(19,4) NOT NULL,
  settled_at TIMESTAMP,
  FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

CREATE INDEX idx_settlements_txn ON transaction_settlements(transaction_id);
CREATE INDEX idx_settlements_currency ON transaction_settlements(original_currency, settlement_currency);
```

**Effort:** 6-7 jours | **Dépendances:** External Exchange Rate API | **Impact:** 🟡 Élevé

---

# PHASE 3: REPORTING & ANALYTICS (Priorité: MOYENNE)
## Semaine 5-6 | Impact: Moyen | Effort: Moyen-Élevé

### 3.1 Reports & Analytics [PRIORITÉ 5]

**À Implémenter:**

#### 3.1.1 Report Service

```java
@Service
public class TransactionReportService {
  
  // Daily summary report
  public DailySummaryReport generateDailySummary(LocalDate date) {
    List<Transaction> dayTransactions = transactionRepository.findByDateAndStatus(
      date,
      TransactionStatus.COMPLETED
    );
    
    return DailySummaryReport.builder()
      .date(date)
      .totalTransactions(dayTransactions.size())
      .successfulTransactions((int) dayTransactions.stream()
        .filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count())
      .failedTransactions((int) dayTransactions.stream()
        .filter(t -> t.getStatus() == TransactionStatus.FAILED).count())
      .totalVolume(dayTransactions.stream()
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add))
      .totalFees(dayTransactions.stream()
        .map(t -> t.getFeeAmount() != null ? t.getFeeAmount() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add))
      .averageTransactionValue(
        dayTransactions.isEmpty()
          ? BigDecimal.ZERO
          : dayTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(dayTransactions.size()), 4, RoundingMode.HALF_UP)
      )
      .currencyBreakdown(dayTransactions.stream()
        .collect(Collectors.groupingBy(
          Transaction::getCurrency,
          Collectors.summingLong(t -> t.getAmount().longValue())
        )))
      .build();
  }
  
  // User activity report
  public UserActivityReport getUserActivityReport(Long userId, LocalDate fromDate, LocalDate toDate) {
    List<Transaction> userTransactions = transactionRepository.findByUserAndDateRange(
      userId, fromDate, toDate
    );
    
    return UserActivityReport.builder()
      .userId(userId)
      .fromDate(fromDate)
      .toDate(toDate)
      .totalTransactions(userTransactions.size())
      .totalSpent(userTransactions.stream()
        .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add))
      .totalReceived(userTransactions.stream()
        .filter(t -> t.getStatus() == TransactionStatus.COMPLETED && t.getSenderWalletNumber().equals(userId))
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add))
      .transactionsByStatus(userTransactions.stream()
        .collect(Collectors.groupingBy(Transaction::getStatus, Collectors.counting())))
      .transactionsByType(userTransactions.stream()
        .collect(Collectors.groupingBy(Transaction::getType, Collectors.counting())))
      .build();
  }
  
  // Merchant analytics
  public MerchantAnalyticsReport getMerchantAnalytics(String merchantWallet, LocalDate fromDate, LocalDate toDate) {
    List<Transaction> merchantTransactions = transactionRepository.findMerchantTransactions(
      merchantWallet, fromDate, toDate
    );
    
    // Calculate chargeback rate, average ticket, etc.
    // ...implementation...
    
    return MerchantAnalyticsReport.builder()
      .merchantWallet(merchantWallet)
      .totalRevenue(merchantTransactions.stream()
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add))
      .transactionCount(merchantTransactions.size())
      .averageTicketSize(merchantTransactions.isEmpty()
        ? BigDecimal.ZERO
        : merchantTransactions.stream()
          .map(Transaction::getAmount)
          .reduce(BigDecimal.ZERO, BigDecimal::add)
          .divide(BigDecimal.valueOf(merchantTransactions.size()), 4, RoundingMode.HALF_UP))
      .chargebackRate(calculateChargebackRate(merchantWallet))
      .topProducts(getMerchantTopProducts(merchantWallet))
      .build();
  }
}
```

#### 3.1.2 Report Controller

```java
@RestController
@RequestMapping("/api/reports")
public class ReportController {
  
  @GetMapping("/daily/{date}")
  public ResponseEntity<DailySummaryReport> getDailySummary(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    DailySummaryReport report = reportService.generateDailySummary(date);
    return ResponseEntity.ok(report);
  }
  
  @GetMapping("/user/{userId}")
  public ResponseEntity<UserActivityReport> getUserActivityReport(
    @PathVariable Long userId,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
  ) {
    UserActivityReport report = reportService.getUserActivityReport(userId, fromDate, toDate);
    return ResponseEntity.ok(report);
  }
  
  @GetMapping("/merchant/{merchantWallet}")
  public ResponseEntity<MerchantAnalyticsReport> getMerchantAnalytics(
    @PathVariable String merchantWallet,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
  ) {
    MerchantAnalyticsReport report = reportService.getMerchantAnalytics(merchantWallet, fromDate, toDate);
    return ResponseEntity.ok(report);
  }
}
```

**Effort:** 5-6 jours | **Dépendances:** Aucune | **Impact:** 🟡 Moyen (insights business)

---

### 3.2 Reconciliation [PRIORITÉ 6]

**À Implémenter:**

#### 3.2.1 Reconciliation Service

```java
@Service
public class ReconciliationService {
  
  // Daily reconciliation
  @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
  public void dailyReconciliation() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    
    // 1. Get all settled transactions for yesterday
    List<Transaction> transactions = transactionRepository.findBySettledDate(yesterday);
    
    // 2. Get settlement records from bank/external system
    List<BankSettlement> bankRecords = bankGateway.getSettlements(yesterday);
    
    // 3. Reconcile: match internal records with bank records
    List<ReconciliationEntry> discrepancies = reconcile(transactions, bankRecords);
    
    // 4. Log discrepancies
    if (!discrepancies.isEmpty()) {
      discrepancies.forEach(entry -> {
        reconciliationLog.logDiscrepancy(entry);
        notificationService.alertReconciliation("DISCREPANCY_FOUND", entry);
      });
    }
    
    // 5. Mark as reconciled
    transactions.forEach(t -> {
      t.setReconciled(true);
      t.setReconciledAt(LocalDateTime.now());
    });
    transactionRepository.saveAll(transactions);
  }
  
  private List<ReconciliationEntry> reconcile(
    List<Transaction> internalRecords,
    List<BankSettlement> bankRecords
  ) {
    List<ReconciliationEntry> discrepancies = new ArrayList<>();
    
    internalRecords.forEach(internal -> {
      BankSettlement match = bankRecords.stream()
        .filter(b -> b.getReferenceNumber().equals(internal.getReference()))
        .findFirst()
        .orElse(null);
      
      if (match == null) {
        discrepancies.add(ReconciliationEntry.builder()
          .type("MISSING_IN_BANK")
          .transactionId(internal.getId())
          .reference(internal.getReference())
          .amount(internal.getAmount())
          .build());
      } else if (!match.getAmount().equals(internal.getAmount())) {
        discrepancies.add(ReconciliationEntry.builder()
          .type("AMOUNT_MISMATCH")
          .transactionId(internal.getId())
          .reference(internal.getReference())
          .expectedAmount(internal.getAmount())
          .actualAmount(match.getAmount())
          .build());
      }
    });
    
    return discrepancies;
  }
}
```

**Effort:** 4-5 jours | **Dépendances:** Bank Gateway API | **Impact:** 🟡 Moyen

---

# PHASE 4: OPTIMISATIONS & FEATURES AVANCÉES (Priorité: BASSE)
## Semaine 7+ | Impact: Bas-Moyen | Effort: Variable

### 4.1 Advanced Routing Strategy [PRIORITÉ 7]

**À Implémenter:**

```java
@Service
public class AdvancedRoutingService {
  
  // Intelligent routing with fallback
  public RoutingDecision selectRoute(Transaction transaction) {
    // 1. Check blacklist
    if (isBlacklisted(transaction.getReceiverWalletNumber())) {
      return RoutingDecision.REJECT;
    }
    
    // 2. Evaluate velocity
    if (exceedsVelocity(transaction.getSenderWalletNumber())) {
      return RoutingDecision.ROUTE_TO_REVIEW;
    }
    
    // 3. Select route based on rules
    RouteOption selectedRoute = null;
    List<RouteOption> availableRoutes = configProperties.getAvailableRoutes();
    
    for (RouteOption route : availableRoutes) {
      if (route.isEligible(transaction)) {
        // Check load balancing
        if (!route.isOverloaded()) {
          selectedRoute = route;
          break;
        }
      }
    }
    
    if (selectedRoute == null) {
      // Fallback to default route
      selectedRoute = configProperties.getDefaultRoute();
    }
    
    return RoutingDecision.builder()
      .route(selectedRoute)
      .reason("Route selected based on transaction characteristics")
      .build();
  }
}
```

### 4.2 Rate Limiting & Throttling [PRIORITÉ 8]

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
  
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
    
    String userId = getUserIdFromToken(request);
    
    // Check rate limit
    if (!rateLimitService.allowRequest(userId)) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
      return;
    }
    
    filterChain.doFilter(request, response);
  }
}
```

### 4.3 Caching Strategy [PRIORITÉ 9]

```java
@Service
@CacheConfig(cacheNames = "transactions")
public class TransactionService {
  
  @Cacheable(key = "#id")
  public Transaction getTransaction(Long id) {
    return transactionRepository.findById(id).orElse(null);
  }
  
  @CacheEvict(key = "#transaction.id")
  public Transaction updateTransaction(Transaction transaction) {
    return transactionRepository.save(transaction);
  }
}
```

---

# 📊 TABLEAU D'IMPLÉMENTATION OPTIMAL

## Ordre Recommandé (16 semaines)

### Phase 1: Fondations (Semaines 1-2)
1. **Advanced Search & Filtering** (3-4 j) → Utilisateurs attendent
2. **Reversal & Refund** (5-6 j) → Critique business

### Phase 2: Gestion Avancée (Semaines 3-5)
3. **Dispute Management** (8-10 j) → Coûteux sans (chargebacks)
4. **Multi-Currency** (6-7 j) → Expansion géographique
5. **Reports & Analytics** (5-6 j) → Insights business

### Phase 3: Infrastructure (Semaines 6-7)
6. **Reconciliation** (4-5 j) → Conformité financière

### Phase 4: Optimisations (Semaines 8+)
7. **Advanced Routing** (3-4 j) → Performances
8. **Rate Limiting** (2-3 j) → Stabilité
9. **Caching** (2-3 j) → Optimisation

---

## Dépendances Entre Fonctionnalités

```
Advanced Search
  ├─ Reversal & Refund ✓
  ├─ Dispute Management
  │  ├─ Multi-Currency ✓
  │  ├─ Reports ✓
  │  └─ Reconciliation ✓
  └─ Routing Strategy ✓

Reversal & Refund
  ├─ Reports ✓
  └─ Reconciliation ✓

Dispute Management
  ├─ Reversal & Refund
  └─ Multi-Currency ✓

Reports & Analytics
  ├─ Advanced Search
  ├─ Multi-Currency ✓
  └─ Reconciliation ✓
```

---

## Estimations Effort Total

| Phase | Semaines | Jours Développement | QA | Déploiement |
|-------|----------|-------------------|----|----|
| Phase 1 | 2 | 9-10j | 2-3j | 1j |
| Phase 2 | 3 | 19-23j | 4-5j | 2j |
| Phase 3 | 1 | 4-5j | 1j | 1j |
| Phase 4 | 2+ | 7-10j | 2-3j | 1j |
| **TOTAL** | **8-9** | **39-48j** | **9-12j** | **5j** |

**Total:** ~16 semaines pour 100% couverture fonctionnelle

---

## Critères de Succès par Phase

### Phase 1
- ✅ Search avec 10+ filtres fonctionnels
- ✅ Reversal/Refund avec état cohérent
- ✅ Tests unitaires 80%+ couverture
- ✅ Déploiement en production sans incidents

### Phase 2
- ✅ Disputes gérables end-to-end
- ✅ Multi-currency avec FX fees
- ✅ Reports fiables et performants

### Phase 3
- ✅ Réconciliation automatique 99%+ match rate
- ✅ Zéro discrepancies non résolues après 24h

### Phase 4
- ✅ Latence moyenne < 200ms
- ✅ Throughput > 1000 txn/sec

---

**Ce plan est actionnable immédiatement. Démarrer par Phase 1, Priorité 1-2.**
