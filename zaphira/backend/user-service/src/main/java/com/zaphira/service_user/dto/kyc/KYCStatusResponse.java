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
 * Response DTO for KYC status check
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCStatusResponse {
    
    private Long kycId;
    private Long userId;
    private KYCStatus status;
    private DocumentType documentType;
    private String documentNumber;
    private LocalDate documentExpiryDate;
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    private String verifiedByName;
    private LocalDateTime rejectedAt;
    private String rejectedByName;
    private String rejectionReason;
    private Integer resubmissionCount;
    private Boolean canResubmit;
    private Boolean isExpired;
    private Boolean canTransact;
    private String message;
}
