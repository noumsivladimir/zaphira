package com.zaphira.service_user.dto.kyc;

import com.zaphira.service_user.model.enums.KYCStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for KYC verification (approval/rejection)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCVerificationResponse {
    
    private Long kycId;
    private Long userId;
    private String userEmail;
    private KYCStatus status;
    private KYCStatus previousStatus;
    private LocalDateTime verifiedAt;
    private String verifiedByName;
    private LocalDateTime rejectedAt;
    private String rejectedByName;
    private String rejectionReason;
    private String notes;
    private String message;
    private Boolean notificationSent;
}
