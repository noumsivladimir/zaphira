package com.zaphira.wallet.service;


import com.zaphira.common.model.enums.PermissionType;
import com.zaphira.wallet.dto.EffectivePermission;
import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.dto.WalletPermissionDTO;
import com.zaphira.wallet.dto.request.GrantPermissionRequest;
import com.zaphira.wallet.dto.request.UpdatePermissionRequest;
import com.zaphira.wallet.models.entities.Wallet;

import java.math.BigDecimal;
import java.util.List;

public interface WalletPermissionService {
    //



     boolean canPerformOperation(Wallet wallet, PermissionType operation, BigDecimal amount) ;





    // Gestion des permissions
    void grantPermission(Long walletId, PermissionType type,
                         BigDecimal maxAmount, BigDecimal dailyLimit);
    void revokePermission(Wallet wallet, PermissionType type);
    void updatePermissionLimit(Wallet wallet, PermissionType type,
                               BigDecimal newLimit);
    void initializeDefaultPermissions (Long walletId );

    WalletPermissionDTO grantPermission(GrantPermissionRequest request);



    BigDecimal getTodayTotal(Wallet wallet, PermissionType operation);


    WalletPermissionDTO updatePermission(Long permissionId, UpdatePermissionRequest request);

    // Consultation
    WalletPermissionDTO getWalletPermissions(String walletNumber);
    List<WalletDTO> getWalletsAccessibleByUser(Long userId);
    WalletPermissionDTO getUserPermissionForWallet(Long userId, String walletNumber);
    List<EffectivePermission> getEffectivePermissions(Wallet wallet);

    List<PermissionType> getPermissionTypes(Long walletId);

    // Validation

    boolean hasPermission(Long walletId, PermissionType permissionType);

    boolean canTransact(Long userId, String walletNumber, java.math.BigDecimal amount);
    boolean canManagePermissions(Long userId, String walletNumber);
    boolean isOwner(Long userId, String walletNumber);

    // Actions sur permissions héritées
    void propagatePermissionToSubWallets(Long permissionId, boolean recursive);
}