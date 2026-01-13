package com.zaphira.wallet.repository;

import com.zaphira.wallet.models.entities.Wallet;
import com.zaphira.wallet.models.entities.WalletPermission;
import com.zaphira.wallet.models.enums.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
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

    /**
     * Trouver toutes les permissions actives d'un wallet
     */
    List<WalletPermission> findByWalletAndEnabled(Wallet wallet, Boolean enabled);

    /**
     * Vérifier si une permission existe et est activée
     */
    boolean existsByWalletAndPermissionTypeAndEnabledTrue(Wallet wallet, PermissionType permissionType);


}
