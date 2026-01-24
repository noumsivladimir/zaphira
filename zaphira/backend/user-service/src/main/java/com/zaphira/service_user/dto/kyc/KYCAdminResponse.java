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
 * Response DTO for admin to view KYC submissions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCAdminResponse {
    
    private Long kycId;
    private Long userId;
    private String userEmail;
    private String userName;
    private KYCStatus status;
    private DocumentType documentType;
    private String documentNumber;
    private LocalDate documentExpiryDate;
    private String fullNameOnDocument;
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    private String verifiedByName;
    private LocalDateTime rejectedAt;
    private String rejectedByName;
    private String rejectionReason;
    private Integer resubmissionCount;
    private Boolean isExpired;
    private Long pendingDays; // Days since submission
}
