package com.zaphira.service_user.services;

import com.zaphira.service_user.dto.kyc.*;
import com.zaphira.service_user.model.enums.KYCStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

@Slf4j
@Service
public class KYCServiceImpl implements KYCService {

    private static final String NOT_IMPLEMENTED_MESSAGE = "KYC not implemented yet";

    @Override
    public KYCSubmissionResponse submitKYC(Long userId,
                                           KYCSubmissionRequest request,
                                           MultipartFile documentFront,
                                           MultipartFile documentBack,
                                           MultipartFile selfie,
                                           MultipartFile proofOfAddress) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    public KYCSubmissionResponse resubmitKYC(Long userId,
                                            Long kycId,
                                            KYCSubmissionRequest request,
                                            MultipartFile documentFront,
                                            MultipartFile documentBack,
                                            MultipartFile selfie,
                                            MultipartFile proofOfAddress) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    public KYCStatusResponse getKYCStatus(Long userId) {
        return KYCStatusResponse.builder()
                .userId(userId)
                .status(KYCStatus.NOT_SUBMITTED)
                .canTransact(false)
                .message(NOT_IMPLEMENTED_MESSAGE)
                .build();
    }

    @Override
    public KYCVerificationResponse approveKYC(Long kycId, Long adminId, String notes) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    public KYCVerificationResponse rejectKYC(Long kycId, Long adminId, String rejectionReason, String notes) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    public KYCStatusResponse markUnderReview(Long kycId, Long adminId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    public Page<KYCAdminResponse> getAllKYCSubmissions(KYCStatus status,
                                                      LocalDate fromDate,
                                                      LocalDate toDate,
                                                      Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Override
    public Page<KYCAdminResponse> getPendingKYCSubmissions(Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Override
    public KYCDetailResponse getKYCDetails(Long kycId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    public int markExpiredDocuments() {
        return 0;
    }

    @Override
    public KYCStatisticsResponse getKYCStatistics() {
        return KYCStatisticsResponse.builder()
                .totalSubmissions(0L)
                .pendingSubmissions(0L)
                .underReviewSubmissions(0L)
                .verifiedSubmissions(0L)
                .rejectedSubmissions(0L)
                .expiredDocuments(0L)
                .verificationRate(0.0)
                .rejectionRate(0.0)
                .avgVerificationTimeHours(0L)
                .oldestPendingDays(0L)
                .build();
    }

    @Override
    public boolean canUserTransact(Long userId) {
        return false;
    }

    @Override
    public TransactionLimitResponse getTransactionLimits(Long userId) {
        return TransactionLimitResponse.builder()
                .userId(userId)
                .kycLevel("NOT_SUBMITTED")
                .isVerified(false)
                .canTransact(false)
                .dailyTransactionLimit(BigDecimal.ZERO)
                .dailySendLimit(BigDecimal.ZERO)
                .dailyReceiveLimit(BigDecimal.ZERO)
                .monthlyTransactionLimit(BigDecimal.ZERO)
                .monthlySendLimit(BigDecimal.ZERO)
                .monthlyReceiveLimit(BigDecimal.ZERO)
                .maxSingleTransaction(BigDecimal.ZERO)
                .minSingleTransaction(BigDecimal.ZERO)
                .canWithdraw(false)
                .canDeposit(false)
                .canTransferInternational(false)
                .requiresAdditionalVerification(true)
                .message(NOT_IMPLEMENTED_MESSAGE)
                .build();
    }

    @Override
    public boolean deleteKYCDocuments(Long kycId, Long adminId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
    }
}
