package com.zaphira.service_user.dto.kyc;

import com.zaphira.service_user.model.enums.DocumentType;
import com.zaphira.service_user.model.enums.KYCStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for detailed KYC view (admin only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCDetailResponse {
    
    private Long kycId;
    private Long userId;
    private String userEmail;
    private String userName;
    private String userPhoneNumber;
    private KYCStatus status;
    private DocumentType documentType;
    private String documentNumber;
    private LocalDate documentIssueDate;
    private LocalDate documentExpiryDate;
    private String documentIssuingCountry;
    private String fullNameOnDocument;
    private LocalDate dateOfBirthOnDocument;
    private String addressOnDocument;
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    private Long verifiedBy;
    private String verifiedByName;
    private LocalDateTime rejectedAt;
    private String rejectedBy;
    private String rejectedByName;
    private String rejectionReason;
    private Integer resubmissionCount;
    private LocalDateTime lastResubmittedAt;
    private String notes;
    private Boolean isExpired;
    
    // Document image URLs
    private String documentFrontImageUrl;
    private String documentBackImageUrl;
    private String selfieImageUrl;
    private String proofOfAddressUrl;
}
