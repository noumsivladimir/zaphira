package com.zaphira.auth.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Participants
    @ManyToOne
    @JoinColumn(name = "sender_wallet_id", nullable = false)
    private Wallet senderWallet;

    @ManyToOne
    @JoinColumn(name = "receiver_wallet_id", nullable = false)
    private Wallet receiverWallet;

    // Montant et devise
    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false, length = 3)
    private String currency;

    // Type de transaction : PAYMENT, TRANSFER, TOPUP, WITHDRAWAL, ESCROW, etc.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // Statut de la transaction : INITIATED, PENDING, AUTHORIZED, PROCESSING, COMPLETED, FAILED, etc.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    // Dates
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime authorizedAt;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;

    // Référence externe ou interne
    @Column(unique = true, nullable = false)
    private String reference;

    // Notes, description ou message
    @Column(length = 255)
    private String description;

    // Méthode d'autorisation : PIN, OTP, BIOMETRIC, 2FA, ADMIN
    @Enumerated(EnumType.STRING)
    private AuthorizationMethod authorizationMethod;

    // Taux de change si multi-devise
    private Double exchangeRate;

    // Frais de transaction
    private Double fee;

    // Indicateur de remboursement ou de réversion
    @Builder.Default
    private Boolean isReversed = false;
    @Builder.Default
    private Boolean isRefunded = false;

    // Auditing
    @Builder.Default
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();


    // Enumérations
    public enum TransactionType {
        PAYMENT, TRANSFER, TOPUP, WITHDRAWAL, ESCROW, SCHEDULED, RECURRING, SPLIT
    }

    public enum TransactionStatus {
        INITIATED, PENDING, AUTHORIZED, PROCESSING, COMPLETED, FAILED,
        CANCELLED, REVERSED, REFUNDED, EXPIRED, ON_HOLD, UNDER_REVIEW
    }

    public enum AuthorizationMethod {
        PIN, OTP, BIOMETRIC, TWO_FA, ADMIN
    }

    // Méthodes utilitaires
    public void markAsCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = TransactionStatus.FAILED;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void authorize(AuthorizationMethod method) {
        this.status = TransactionStatus.AUTHORIZED;
        this.authorizationMethod = method;
        this.authorizedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }
}
