package com.zaphira.transaction.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transaction_metadata")
@Getter
@Setter
public class TransactionMetadata {

    @Id
    private Long transactionId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(columnDefinition = "JSONB")
    private String metadata;
}