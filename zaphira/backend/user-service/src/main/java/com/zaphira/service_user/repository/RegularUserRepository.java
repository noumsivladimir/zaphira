
package com.zaphira.service_user.repository;

import com.zaphira.service_user.model.entities.RegularUser;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegularUserRepository extends JpaRepository<RegularUser, Long> {

    Optional<RegularUser> findByEmail(String email);

    Optional<RegularUser> findByUserId(Long userId);

    List<RegularUser> findByPreferredCurrency(String currency);

    List<RegularUser> findByPreferredLanguage(String language);

        Optional<RegularUser> findByPhoneNumber(String phoneNumber);
//
//    @Query("SELECT u FROM RegularUser u WHERE u.KYC.kycStatus = :status")
//    List<RegularUser> findByKycStatus(@Param("status") KYCStatus status);
//
//    @Query("SELECT COUNT(u) FROM RegularUser u WHERE u.kyc.kycStatus = 'VERIFIED'")
//    Long countVerifiedUsers();

    @Query("SELECT COUNT(u) FROM RegularUser u WHERE u.kycVerifiedAt IS NOT NULL")
    Long countKycVerifiedUsers();

    @Query("SELECT u FROM RegularUser u WHERE u.lastTransactionAt IS NULL OR u.lastTransactionAt < :date")
    List<RegularUser> findInactiveUsers(@Param("date") LocalDateTime date);
}