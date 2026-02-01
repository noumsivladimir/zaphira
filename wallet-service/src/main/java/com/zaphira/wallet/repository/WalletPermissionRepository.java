package com.zaphira.wallet.repository;

import com.zaphira.common.model.enums.PermissionType;
import com.zaphira.wallet.models.entities.Wallet;
import com.zaphira.wallet.models.entities.WalletPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletPermissionRepository extends JpaRepository<WalletPermission, Long> {

    /** Trouver une permission spécifique pour un wallet
     * Spring Data JPA génère automatiquement la requête
     */
    Optional<WalletPermission> findByWalletAndPermissionType(Wallet wallet, PermissionType permissionType);

    /**
     * Alternative avec ID du wallet
     */
    Optional<WalletPermission> findByWalletIdAndPermissionType(Long walletId, PermissionType permissionType);

    /**
     * Trouver toutes les permissions d'un wallet
     */
    List<WalletPermission> findByWallet(Wallet wallet);

    @Query("""
    SELECT CASE WHEN COUNT(wp) > 0 THEN true ELSE false END
    FROM WalletPermission wp
    WHERE wp.wallet.id = :walletId
      AND wp.permissionType = :permissionType
      AND wp.enabled = true
    """)
    boolean hasActivePermission(@Param("walletId") Long walletId,
                                @Param("permissionType") PermissionType permissionType);

    /**
     * Trouver toutes les permissions actives d'un wallet
     */
    List<WalletPermission> findByWalletAndEnabled(Wallet wallet, Boolean enabled);

    @Query("SELECT wp.permissionType FROM WalletPermission wp " +
            "WHERE wp.wallet.id = :walletId AND wp.enabled = true")
    List<PermissionType> findPermissionTypesByWalletId(@Param("walletId") Long walletId);

    /**
     * Vérifier si une permission existe et est activée
     */
    boolean existsByWalletAndPermissionTypeAndEnabledTrue(Wallet wallet, PermissionType permissionType);


}
