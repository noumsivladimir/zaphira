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
 * Response DTO for KYC submission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCSubmissionResponse {
    
    private Long kycId;
    private Long userId;
    private KYCStatus status;
    private DocumentType documentType;
    private String documentNumber;
    private LocalDate documentExpiryDate;
    private String message;
    private LocalDateTime submittedAt;
    private Integer resubmissionCount;
    private Boolean canResubmit;
    
    // Image URLs (for display)
    private String documentFrontImageUrl;
    private String documentBackImageUrl;
    private String selfieImageUrl;
    private String proofOfAddressUrl;
}
