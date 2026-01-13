package com.zaphira.transaction.model.entities;

import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.model.enums.AuthorizationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_authorizations")
@Getter @Setter
public class TransactionAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    private boolean required;

    private Integer requiredLevel;

    @Enumerated(EnumType.STRING)
    private AuthorizationMethod requestedMethod;

    @Enumerated(EnumType.STRING)
    private AuthorizationMethod actualMethod;

    private LocalDateTime authorizedAt;

    @Enumerated(EnumType.STRING)
    private AuthorizationStatus status;
}