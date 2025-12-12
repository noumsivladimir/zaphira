package com.zaphira.service_user.repository;

import com.zaphira.service_user.model.entities.AdminUser;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByEmail(String email);

    Optional<AdminUser> findByUserId(Long userId);

    Optional<AdminUser> findByEmployeeId(String employeeId);

   // List<AdminUser> findByPosition(String position);

    @Query("SELECT a FROM AdminUser a WHERE a.adminLevel = 'SUPER_ADMIN'")
    List<AdminUser> findAllSuperAdmins();

    @Query("SELECT a FROM AdminUser a WHERE a.adminLevel = 'SENIOR'")
    List<AdminUser> findAllSeniorAdmins();

    @Query("SELECT a FROM AdminUser a WHERE a.supervisorId = :supervisorId")
    List<AdminUser> findBySupervisor(@Param("supervisorId") Long supervisorId);

    @Query("SELECT a FROM AdminUser a WHERE a.canManageUsers = true")
    List<AdminUser> findAdminsWhoCanManageUsers();

    @Query("SELECT a FROM AdminUser a WHERE a.canApproveTransactions = true")
    List<AdminUser> findAdminsWhoCanApproveTransactions();

    boolean existsByEmployeeId(String employeeId);
}