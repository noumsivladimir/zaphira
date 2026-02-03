package com.zaphira.transaction.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * TransactionAuthorizationInfo - Authorization method details
 * Stores information about how the transaction was authorized
 * Separate from TransactionAuthorization which handles the authorization process itself
 */
@Entity
@Table(name = "transaction_authorization_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionAuthorizationInfo {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    /** Authorization method used */
    @Column(name = "authorization_method", length = 20)
    private String authorizationMethod;

    @Column(name = "actual_authorization_method", length = 20)
    private String actualAuthorizationMethod;

    @Column(name = "authorization_required")
    @Builder.Default
    private Boolean authorizationRequired = false;

    @Column(name = "authorization_level", length = 20)
    private String authorizationLevel;

    @Column(name = "phone_number_used_for_auth", length = 20)
    private String phoneNumberUsedForAuth;

    /** Audit */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
