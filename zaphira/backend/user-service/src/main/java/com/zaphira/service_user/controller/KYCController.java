package com.zaphira.service_user.controller;

import com.zaphira.service_user.dto.kyc.*;
import com.zaphira.service_user.model.enums.KYCStatus;
import com.zaphira.service_user.services.KYCService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * Controller for KYC operations
 * Handles user KYC submission and admin verification workflow
 */
@Slf4j
@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KYCController {

    private final KYCService kycService;

    /**
     * Submit KYC documents (User endpoint)
     * POST /api/kyc/submit
     */
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT')")
    public ResponseEntity<KYCSubmissionResponse> submitKYC(
            @RequestAttribute("userId") Long userId,
            @Valid @ModelAttribute KYCSubmissionRequest request,
            @RequestParam("documentFront") MultipartFile documentFront,
            @RequestParam(value = "documentBack", required = false) MultipartFile documentBack,
            @RequestParam("selfie") MultipartFile selfie,
            @RequestParam("proofOfAddress") MultipartFile proofOfAddress) {
        
        log.info("KYC submission request from user {}", userId);
        
        KYCSubmissionResponse response = kycService.submitKYC(
                userId, request, documentFront, documentBack, selfie, proofOfAddress);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Resubmit KYC documents after rejection (User endpoint)
     * PUT /api/kyc/{kycId}/resubmit
     */
    @PutMapping(value = "/{kycId}/resubmit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT')")
    public ResponseEntity<KYCSubmissionResponse> resubmitKYC(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long kycId,
            @Valid @ModelAttribute KYCSubmissionRequest request,
            @RequestParam("documentFront") MultipartFile documentFront,
            @RequestParam(value = "documentBack", required = false) MultipartFile documentBack,
            @RequestParam("selfie") MultipartFile selfie,
            @RequestParam("proofOfAddress") MultipartFile proofOfAddress) {
        
        log.info("KYC resubmission request from user {} for KYC {}", userId, kycId);
        
        KYCSubmissionResponse response = kycService.resubmitKYC(
                userId, kycId, request, documentFront, documentBack, selfie, proofOfAddress);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get KYC status for current user
     * GET /api/kyc/status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT')")
    public ResponseEntity<KYCStatusResponse> getMyKYCStatus(
            @RequestAttribute("userId") Long userId) {
        
        log.debug("Fetching KYC status for user {}", userId);
        
        KYCStatusResponse response = kycService.getKYCStatus(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction limits based on KYC level
     * GET /api/kyc/limits
     */
    @GetMapping("/limits")
    @PreAuthorize("hasRole('USER') or hasRole('MERCHANT')")
    public ResponseEntity<TransactionLimitResponse> getMyTransactionLimits(
            @RequestAttribute("userId") Long userId) {
        
        log.debug("Fetching transaction limits for user {}", userId);
        
        TransactionLimitResponse response = kycService.getTransactionLimits(userId);
        return ResponseEntity.ok(response);
    }

    // ==================== Admin Endpoints ====================

    /**
     * Get all KYC submissions with filters (Admin endpoint)
     * GET /api/kyc/admin/submissions
     */
    @GetMapping("/admin/submissions")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MANAGE_KYC')")
    public ResponseEntity<Page<KYCAdminResponse>> getAllKYCSubmissions(
            @RequestParam(required = false) KYCStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "submittedAt,desc") String sort) {
        
        log.info("Admin fetching KYC submissions - status: {}, from: {}, to: {}", status, fromDate, toDate);
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
        
        Page<KYCAdminResponse> submissions = kycService.getAllKYCSubmissions(
                status, fromDate, toDate, pageable);
        
        return ResponseEntity.ok(submissions);
    }

    /**
     * Get pending KYC submissions (Admin endpoint)
     * GET /api/kyc/admin/pending
     */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MANAGE_KYC')")
    public ResponseEntity<Page<KYCAdminResponse>> getPendingKYCSubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Admin fetching pending KYC submissions");
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "submittedAt"));
        Page<KYCAdminResponse> pendingSubmissions = kycService.getPendingKYCSubmissions(pageable);
        
        return ResponseEntity.ok(pendingSubmissions);
    }

    /**
     * Get detailed KYC information (Admin endpoint)
     * GET /api/kyc/admin/{kycId}
     */
    @GetMapping("/admin/{kycId}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('VIEW_ALL_KYC')")
    public ResponseEntity<KYCDetailResponse> getKYCDetails(@PathVariable Long kycId) {
        
        log.info("Admin fetching KYC details for ID: {}", kycId);
        
        KYCDetailResponse details = kycService.getKYCDetails(kycId);
        return ResponseEntity.ok(details);
    }

    /**
     * Approve a KYC submission (Admin endpoint)
     * POST /api/kyc/admin/{kycId}/approve
     */
    @PostMapping("/admin/{kycId}/approve")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('APPROVE_KYC')")
    public ResponseEntity<KYCVerificationResponse> approveKYC(
            @RequestAttribute("userId") Long adminId,
            @PathVariable Long kycId,
            @RequestParam(required = false) String notes) {
        
        log.info("Admin {} approving KYC {}", adminId, kycId);
        
        KYCVerificationResponse response = kycService.approveKYC(kycId, adminId, notes);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a KYC submission (Admin endpoint)
     * POST /api/kyc/admin/{kycId}/reject
     */
    @PostMapping("/admin/{kycId}/reject")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('REJECT_KYC')")
    public ResponseEntity<KYCVerificationResponse> rejectKYC(
            @RequestAttribute("userId") Long adminId,
            @PathVariable Long kycId,
            @RequestParam String rejectionReason,
            @RequestParam(required = false) String notes) {
        
        log.info("Admin {} rejecting KYC {} with reason: {}", adminId, kycId, rejectionReason);
        
        KYCVerificationResponse response = kycService.rejectKYC(kycId, adminId, rejectionReason, notes);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark KYC as under review (Admin endpoint)
     * POST /api/kyc/admin/{kycId}/review
     */
    @PostMapping("/admin/{kycId}/review")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MANAGE_KYC')")
    public ResponseEntity<KYCStatusResponse> markUnderReview(
            @RequestAttribute("userId") Long adminId,
            @PathVariable Long kycId) {
        
        log.info("Admin {} marking KYC {} as under review", adminId, kycId);
        
        KYCStatusResponse response = kycService.markUnderReview(kycId, adminId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get KYC statistics (Admin endpoint)
     * GET /api/kyc/admin/statistics
     */
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('VIEW_ALL_KYC')")
    public ResponseEntity<KYCStatisticsResponse> getKYCStatistics() {
        
        log.info("Admin fetching KYC statistics");
        
        KYCStatisticsResponse statistics = kycService.getKYCStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Delete KYC documents (GDPR compliance - Admin endpoint)
     * DELETE /api/kyc/admin/{kycId}/documents
     */
    @DeleteMapping("/admin/{kycId}/documents")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MANAGE_KYC')")
    public ResponseEntity<Void> deleteKYCDocuments(
            @RequestAttribute("userId") Long adminId,
            @PathVariable Long kycId) {
        
        log.warn("Admin {} deleting KYC documents for KYC {}", adminId, kycId);
        
        kycService.deleteKYCDocuments(kycId, adminId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if user can transact (Internal/Service endpoint)
     * GET /api/kyc/can-transact/{userId}
     */
    @GetMapping("/can-transact/{userId}")
    public ResponseEntity<Boolean> canUserTransact(@PathVariable Long userId) {
        
        log.debug("Checking transaction eligibility for user {}", userId);
        
        boolean canTransact = kycService.canUserTransact(userId);
        return ResponseEntity.ok(canTransact);
    }

    /**
     * Get transaction limits for a user (Internal/Service endpoint)
     * GET /api/kyc/limits/{userId}
     */
    @GetMapping("/limits/{userId}")
    public ResponseEntity<TransactionLimitResponse> getUserTransactionLimits(@PathVariable Long userId) {
        
        log.debug("Fetching transaction limits for user {}", userId);
        
        TransactionLimitResponse response = kycService.getTransactionLimits(userId);
        return ResponseEntity.ok(response);
    }
}
