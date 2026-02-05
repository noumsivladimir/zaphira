package com.zaphira.wallet.service;


import com.zaphira.wallet.dto.EffectivePermission;
import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.dto.WalletPermissionDTO;
import com.zaphira.wallet.dto.request.UpdatePermissionRequest;
import com.zaphira.wallet.model.entities.Wallet;
import com.zaphira.wallet.model.enums.PermissionType;

import java.math.BigDecimal;
import java.util.List;

public interface WalletPermissionService {
    //



     boolean canPerformOperation(Wallet wallet, PermissionType operation, BigDecimal amount) ;





    // Gestion des permissions
    void grantPermission(Long walletId, PermissionType type,
                         BigDecimal maxAmount, BigDecimal dailyLimit);
    void revokePermission(Wallet wallet, PermissionType type);
    void revokePermissionById(Long permissionId);
    void updatePermissionLimit(Wallet wallet, PermissionType type,
                               BigDecimal newLimit);
    void initializeDefaultPermissions (Long walletId );

    void customizePermission(Wallet wallet, PermissionType type,
                             BigDecimal maxAmount, BigDecimal dailyLimit,
                             Boolean requiresApproval);


    BigDecimal getTodayTotal(Wallet wallet, PermissionType operation);


    WalletPermissionDTO updatePermission(Long permissionId, UpdatePermissionRequest request);

    // Consultation
    List<WalletPermissionDTO> getWalletPermissions(String walletNumber);
    List<com.zaphira.wallet.dto.response.WalletPermissionResponse> getPermissionsByWalletId(Long walletId);
    List<WalletDTO> getWalletsAccessibleByUser(Long userId);
    WalletPermissionDTO getUserPermissionForWallet(Long userId, String walletNumber);
    List<EffectivePermission> getEffectivePermissions(Wallet wallet);

    // Validation
    boolean hasPermission(Long userId, String walletNumber, String action);
    boolean canTransact(Long userId, String walletNumber, java.math.BigDecimal amount);
    boolean canManagePermissions(Long userId, String walletNumber);
    boolean isOwner(Long userId, String walletNumber);

    // Actions sur permissions héritées
    void propagatePermissionToSubWallets(Long permissionId, boolean recursive);
}