package com.zaphira.service_user.model.entities;

import com.zaphira.service_user.model.enums.DocumentType;
import com.zaphira.service_user.model.enums.KYCStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;



@Entity
@Table(name = "kyc", indexes = {
        @Index(name = "idx_kyc_user_id", columnList = "user_id"),
        @Index(name = "idx_kyc_status", columnList = "kyc_status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYC {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long kycId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KYCStatus kycStatus = KYCStatus.NOT_SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DocumentType documentType;

    @Column(length = 100)
    private String documentNumber;

    @Column
    private LocalDate documentIssueDate;

    @Column
    private LocalDate documentExpiryDate;

    @Column(length = 100)
    private String documentIssuingCountry;

    @Column(length = 500)
    private String documentFrontImage;

    @Column(length = 500)
    private String documentBackImage;

    @Column(length = 500)
    private String selfieImage;

    @Column(length = 500)
    private String proofOfAddressDocument;

    @Column(length = 200)
    private String fullNameOnDocument;

    @Column
    private LocalDate dateOfBirthOnDocument;

    @Column(length = 500)
    private String addressOnDocument;

    @Column
    private LocalDateTime submittedAt;

    @Column
    private LocalDateTime verifiedAt;

    @Column
    private Long verifiedBy;

    @Column(length = 1000)
    private String rejectionReason;

    @Column
    private LocalDateTime rejectedAt;

    @Column
    private String rejectedBy;

    @Column(nullable = false)
    private Integer resubmissionCount = (Integer) 0;

    @Column
    private LocalDateTime lastResubmittedAt;

    @Column(length = 1000)
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public boolean isVerified() {
        return kycStatus == KYCStatus.VERIFIED;
    }

    public boolean isPending() {
        return kycStatus == KYCStatus.PENDING || kycStatus == KYCStatus.UNDER_REVIEW;
    }

    public boolean isRejected() {
        return kycStatus == KYCStatus.REJECTED;
    }

    public boolean isExpired() {
        if (documentExpiryDate == null) {
            return false;
        }
        return documentExpiryDate.isBefore(LocalDate.now());
    }

    public boolean canResubmit() {
        return kycStatus == KYCStatus.REJECTED && resubmissionCount < 3;
    }

    public void markAsSubmitted() {
        this.kycStatus = KYCStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
    }

    public void markAsUnderReview() {
        this.kycStatus = KYCStatus.UNDER_REVIEW;
    }

    public void markAsVerified(Long adminId) {
        this.kycStatus = KYCStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = adminId;
    }

    public void markAsRejected(AdminUser adminUser, String reason) {
        this.kycStatus = KYCStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.rejectedBy = adminUser.getFullName();
        this.rejectionReason = reason;
    }
    
    public void markAsRejected(String adminIdentifier, String reason) {
        this.kycStatus = KYCStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.rejectedBy = adminIdentifier;
        this.rejectionReason = reason;
    }

    public void incrementResubmissionCount() {
        this.resubmissionCount++;
        this.lastResubmittedAt = LocalDateTime.now();
    }
}