package com.zaphira.service_user.services;

import com.zaphira.service_user.dto.kyc.*;
import com.zaphira.service_user.model.entities.KYC;
import com.zaphira.service_user.model.enums.KYCStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for KYC (Know Your Customer) operations
 * Handles document submission, verification, and status management
 */
public interface KYCService {
    
    /**
     * Submit KYC documents for a user
     * 
     * @param userId User ID submitting KYC
     * @param request KYC submission request with document details
     * @param documentFront Front image of document
     * @param documentBack Back image of document (optional)
     * @param selfie Selfie image for verification
     * @param proofOfAddress Proof of address document
     * @return KYCSubmissionResponse with submission details
     */
    KYCSubmissionResponse submitKYC(
        Long userId,
        KYCSubmissionRequest request,
        MultipartFile documentFront,
        MultipartFile documentBack,
        MultipartFile selfie,
        MultipartFile proofOfAddress
    );
    
    /**
     * Resubmit KYC documents after rejection
     * 
     * @param userId User ID resubmitting KYC
     * @param kycId KYC ID to resubmit
     * @param request Updated KYC submission request
     * @param documentFront New front image of document
     * @param documentBack New back image of document (optional)
     * @param selfie New selfie image
     * @param proofOfAddress New proof of address document
     * @return KYCSubmissionResponse with resubmission details
     */
    KYCSubmissionResponse resubmitKYC(
        Long userId,
        Long kycId,
        KYCSubmissionRequest request,
        MultipartFile documentFront,
        MultipartFile documentBack,
        MultipartFile selfie,
        MultipartFile proofOfAddress
    );
    
    /**
     * Get KYC status and details for a user
     * 
     * @param userId User ID
     * @return KYCStatusResponse with current KYC status
     */
    KYCStatusResponse getKYCStatus(Long userId);
    
    /**
     * Admin: Approve a KYC submission
     * 
     * @param kycId KYC ID to approve
     * @param adminId Admin ID performing approval
     * @param notes Optional admin notes
     * @return KYCVerificationResponse with approval details
     */
    KYCVerificationResponse approveKYC(Long kycId, Long adminId, String notes);
    
    /**
     * Admin: Reject a KYC submission
     * 
     * @param kycId KYC ID to reject
     * @param adminId Admin ID performing rejection
     * @param rejectionReason Reason for rejection
     * @param notes Optional admin notes
     * @return KYCVerificationResponse with rejection details
     */
    KYCVerificationResponse rejectKYC(Long kycId, Long adminId, String rejectionReason, String notes);
    
    /**
     * Admin: Mark KYC as under review
     * 
     * @param kycId KYC ID to mark under review
     * @param adminId Admin ID performing action
     * @return KYCStatusResponse with updated status
     */
    KYCStatusResponse markUnderReview(Long kycId, Long adminId);
    
    /**
     * Admin: Get all KYC submissions with filters
     * 
     * @param status Optional status filter
     * @param fromDate Optional start date filter
     * @param toDate Optional end date filter
     * @param pageable Pagination details
     * @return Page of KYC submissions
     */
    Page<KYCAdminResponse> getAllKYCSubmissions(
        KYCStatus status,
        LocalDate fromDate,
        LocalDate toDate,
        Pageable pageable
    );
    
    /**
     * Admin: Get pending KYC submissions
     * 
     * @param pageable Pagination details
     * @return Page of pending KYC submissions
     */
    Page<KYCAdminResponse> getPendingKYCSubmissions(Pageable pageable);
    
    /**
     * Admin: Get KYC submission details
     * 
     * @param kycId KYC ID
     * @return KYCDetailResponse with full details
     */
    KYCDetailResponse getKYCDetails(Long kycId);
    
    /**
     * Check for expired KYC documents and update status
     * Scheduled job to run daily
     * 
     * @return Count of expired documents updated
     */
    int markExpiredDocuments();
    
    /**
     * Get KYC statistics
     * 
     * @return KYCStatisticsResponse with counts and metrics
     */
    KYCStatisticsResponse getKYCStatistics();
    
    /**
     * Validate if user can perform transactions based on KYC status
     * 
     * @param userId User ID
     * @return true if user can transact, false otherwise
     */
    boolean canUserTransact(Long userId);
    
    /**
     * Get transaction limit based on KYC status
     * 
     * @param userId User ID
     * @return Transaction limit based on verification level
     */
    TransactionLimitResponse getTransactionLimits(Long userId);
    
    /**
     * Delete KYC documents (GDPR compliance)
     * 
     * @param kycId KYC ID
     * @param adminId Admin ID performing deletion
     * @return true if deleted successfully
     */
    boolean deleteKYCDocuments(Long kycId, Long adminId);
}
