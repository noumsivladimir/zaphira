package com.zaphira.wallet.service;

//
//public interface WalletPermissionService {
//
//    // Gestion des permissions
//    WalletPermissionDTO grantPermission(GrantPermissionRequest request);
//    void revokePermission(Long permissionId, Long requestingUserId);
//    WalletPermissionDTO updatePermission(Long permissionId, UpdatePermissionRequest request);
//
//    // Consultation
//    List<WalletPermissionDTO> getWalletPermissions(String walletNumber);
//    List<WalletDTO> getWalletsAccessibleByUser(Long userId);
//    WalletPermissionDTO getUserPermissionForWallet(Long userId, String walletNumber);
//
//    // Validation
//    boolean hasPermission(Long userId, String walletNumber, String action);
//    boolean canTransact(Long userId, String walletNumber, java.math.BigDecimal amount);
//    boolean canManagePermissions(Long userId, String walletNumber);
//    boolean isOwner(Long userId, String walletNumber);
//
//    // Actions sur permissions héritées
//    void propagatePermissionToSubWallets(Long permissionId, boolean recursive);
//}