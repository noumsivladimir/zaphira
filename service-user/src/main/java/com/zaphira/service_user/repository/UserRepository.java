package com.zaphira.service_user.repository;

import com.zaphira.service_user.model.entities.User;
import com.zaphira.service_user.model.enums.AccountStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByWalletId(String walletId);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByWalletId(String walletId);

    List<User> findByAccountStatus(AccountStatus status);

    Page<User> findByAccountStatus(AccountStatus status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.accountStatus = :status AND u.registrationDate >= :date")
    List<User> findRecentUsersByStatus(@Param("status") AccountStatus status,
                                       @Param("date") LocalDateTime date);

    @Query("SELECT u FROM User u WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil < :now")
    List<User> findExpiredLockedAccounts(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(u) FROM User u WHERE u.accountStatus = :status")
    Long countByAccountStatus(@Param("status") AccountStatus status);



    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.registrationDate >= :startDate AND u.registrationDate <= :endDate")
    Long countNewUsersBetween(@Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);
}