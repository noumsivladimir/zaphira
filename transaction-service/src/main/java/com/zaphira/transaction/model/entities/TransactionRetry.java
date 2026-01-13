package com.zaphira.transaction.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transaction_retry")
@Getter
@Setter
public class TransactionRetry {

    @Id
    private Long transactionId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    private Integer retryCount;

    private Integer maxRetry;

    private String lastFailureReason;
}