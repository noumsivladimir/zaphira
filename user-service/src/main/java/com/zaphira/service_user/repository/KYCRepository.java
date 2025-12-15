package com.zaphira.service_user.repository;

import com.zaphira.service_user.model.entities.KYC;
import com.zaphira.service_user.model.entities.User;
import com.zaphira.service_user.model.enums.KYCStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KYCRepository extends JpaRepository<KYC, Long> {

    Optional<KYC> findByUser(User user);

    Optional<KYC> findByUser_UserId(Long userId);

    List<KYC> findByKycStatus(KYCStatus status);

    Page<KYC> findByKycStatus(KYCStatus status, Pageable pageable);

    @Query("SELECT k FROM KYC k WHERE k.kycStatus = 'PENDING' OR k.kycStatus = 'UNDER_REVIEW'")
    List<KYC> findPendingKYCs();

    @Query("SELECT k FROM KYC k WHERE k.kycStatus = 'PENDING' OR k.kycStatus = 'UNDER_REVIEW'")
    Page<KYC> findPendingKYCs(Pageable pageable);

    @Query("SELECT k FROM KYC k WHERE k.kycStatus = 'VERIFIED' AND k.documentExpiryDate < :date")
    List<KYC> findExpiredKYCs(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(k) FROM KYC k WHERE k.kycStatus = :status")
    Long countByStatus(@Param("status") KYCStatus status);

    @Query("SELECT k FROM KYC k WHERE k.submittedAt >= :startDate AND k.submittedAt <= :endDate")
    List<KYC> findBySubmittedDateBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT k FROM KYC k WHERE k.verifiedBy = :adminId")
    List<KYC> findByVerifiedBy(@Param("adminId") Long adminId);

    @Query("SELECT k FROM KYC k WHERE k.rejectedBy = :adminId")
    List<KYC> findByRejectedBy(@Param("adminId") Long adminId);

    @Query("SELECT COUNT(k) FROM KYC k WHERE k.verifiedAt >= :startDate AND k.verifiedAt <= :endDate")
    Long countVerifiedBetween(@Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);
}