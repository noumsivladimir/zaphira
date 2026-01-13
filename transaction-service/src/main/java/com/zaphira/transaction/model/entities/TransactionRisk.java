package com.zaphira.transaction.model.entities;

import com.zaphira.transaction.model.enums.ComplianceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transaction_risk")
@Getter
@Setter
public class TransactionRisk {

    @Id
    private Long transactionId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

//    private Integer riskScore;

    private String ipAddress;

    private String deviceInfo;

    @Enumerated(EnumType.STRING)
    private ComplianceStatus complianceStatus;
}