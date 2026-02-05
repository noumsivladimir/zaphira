package com.zaphira.user.dto.response;


import com.zaphira.user.model.entities.AdminUser;
import com.zaphira.user.model.enums.DocumentType;
import com.zaphira.user.model.enums.KYCStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCStatusResponse {

    private Long kycId;
    private Long userId;
    private KYCStatus kycStatus;
    private DocumentType documentType;
    private String documentNumber;
    private LocalDate documentIssueDate;
    private LocalDate documentExpiryDate;
    private String documentIssuingCountry;
    private String fullNameOnDocument;
    private LocalDate dateOfBirthOnDocument;
    private String addressOnDocument;

    private String documentFrontImage;
    private String documentBackImage;
    private String selfieImage;
   // private String proofOfAddressDocument;

    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    private AdminUser verifiedBy;
    private String rejectionReason;
    private LocalDateTime rejectedAt;
    private Long rejectedBy;

    private Integer resubmissionCount;
    private Boolean canResubmit;
    private Boolean isExpired;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
