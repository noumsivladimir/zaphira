package com.zaphira.service_user.repository;

import com.zaphira.service_user.model.entities.MerchantUser;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantUserRepository extends JpaRepository<MerchantUser, Long> {

    Optional<MerchantUser> findByEmail(String email);

    Optional<MerchantUser> findByUserId(Long userId);

   // Optional<MerchantUser> findByWalletId(Long WalletId);


    Optional<MerchantUser> findByBusinessRegistrationNumber(String registrationNumber);

    List<MerchantUser> findByIsVerifiedMerchant(Boolean isVerified);

    List<MerchantUser> findByVerifiedBy(String verifiedBy);

    Page<MerchantUser> findByIsVerifiedMerchant(Boolean isVerified, Pageable pageable);

    @Query("SELECT m FROM MerchantUser m WHERE m.canAcceptPayments = true AND m.isVerifiedMerchant = true")
    List<MerchantUser> findActiveMerchants();

    @Query("SELECT COUNT(m) FROM MerchantUser m WHERE m.isVerifiedMerchant = true")
    Long countVerifiedMerchants();

    @Query("SELECT m FROM MerchantUser m WHERE m.isVerifiedMerchant = false AND m.accountStatus = 'PENDING_VERIFICATION'")
    List<MerchantUser> findPendingVerification();

    @Query("SELECT m FROM MerchantUser m WHERE m.businessName LIKE %:keyword% OR m.verifiedBy LIKE %:keyword%")
    Page<MerchantUser> searchMerchants(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByBusinessRegistrationNumber(String registrationNumber);

}