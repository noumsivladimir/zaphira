package com.zaphira.transaction.model.entities;

import com.zaphira.common.model.enums.Currency;
import com.zaphira.transaction.model.enums.TransactionCategory;
import com.zaphira.transaction.model.enums.TransactionChannel;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_reference", columnList = "reference", unique = true),
        @Index(name = "idx_tx_sender_wallet", columnList = "sender_wallet_id"),
        @Index(name = "idx_tx_receiver_wallet", columnList = "receiver_wallet_id"),
        @Index(name = "idx_tx_user", columnList = "user_id"),
        @Index(name = "idx_tx_status", columnList = "status"),
        @Index(name = "idx_tx_type", columnList = "type"),
        @Index(name = "idx_tx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Référence métier immutable */
    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    /** Acteurs */

//    @Column(name = "user_id", nullable = false)
//    private Long userId;


    @Column(name = "sender_wallet_id", updatable = false)
    private Long senderWalletId;

    @Column(name = "sender_wallet_number", updatable = false)
    private String senderWalletNumber;

    @Column(name = "receiver_wallet_id", updatable = false)
    private Long receiverWalletId;

    @Column(name = "receiver_wallet_number", updatable = false)
    private String receiverWalletNumber;



    /** Montants */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /** Typologie */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionChannel channel;

    /** État courant (la vérité est dans l’historique) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    /** Métadonnées métier */
    @Column(length = 255)
    private String description;

    @Column(name = "related_transaction_id")
    private Long relatedTransactionId;

    @Column(name = "failure_reason")
    private String failureReason;

    /** Audit */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}


