package com.zaphira.service_user.dto.kyc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for KYC statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCStatisticsResponse {
    
    private Long totalSubmissions;
    private Long pendingSubmissions;
    private Long underReviewSubmissions;
    private Long verifiedSubmissions;
    private Long rejectedSubmissions;
    private Long expiredDocuments;
    private Double verificationRate; // Percentage of verified vs total
    private Double rejectionRate; // Percentage of rejected vs total
    private Long avgVerificationTimeHours; // Average time to verify
    private Long oldestPendingDays; // Days since oldest pending submission
}
