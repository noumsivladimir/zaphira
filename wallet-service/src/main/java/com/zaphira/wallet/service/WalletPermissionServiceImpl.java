package com.zaphira.wallet.service;

import com.zaphira.wallet.dto.EffectivePermission;
import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.dto.WalletPermissionDTO;
import com.zaphira.wallet.dto.request.UpdatePermissionRequest;
import com.zaphira.wallet.exception.WalletNotFoundException;
import com.zaphira.wallet.model.entities.Wallet;
import com.zaphira.wallet.model.entities.WalletPermission;
import com.zaphira.wallet.model.enums.PermissionType;
import com.zaphira.wallet.model.enums.WalletStatus;
import com.zaphira.wallet.model.enums.WalletType;
import com.zaphira.wallet.repository.WalletPermissionRepository;
import com.zaphira.wallet.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;


@Service
@Transactional
@Slf4j
public class WalletPermissionServiceImpl implements WalletPermissionService {

    private final WalletPermissionRepository permissionRepository;
    private final WalletRepository walletRepository;
    private final WalletQueryService walletQueryService;

    public WalletPermissionServiceImpl(WalletPermissionRepository permissionRepository, 
                                      WalletRepository walletRepository,
                                      WalletQueryService walletQueryService) {
        this.permissionRepository = permissionRepository;
        this.walletRepository = walletRepository;
        this.walletQueryService = walletQueryService;
    }


    private static final Map<WalletType, Set<PermissionType>> DEFAULT_PERMISSIONS = Map.of(
            WalletType.USER, Set.of(
                    PermissionType.SEND_MONEY,
                    PermissionType.RECEIVE_MONEY,
                    PermissionType.RECEIVE_DEPOSIT,
                    PermissionType.WITHDRAW_CASH,
                    PermissionType.PAY_MERCHANT

            ),
            WalletType.MERCHANT, Set.of(
                    PermissionType.SEND_MONEY,
                    PermissionType.RECEIVE_MONEY,
                    PermissionType.ACCEPT_PAYMENT,
                    PermissionType.REQUEST_PAYMENT,
                    PermissionType.ISSUE_REFUND,
                    PermissionType.WITHDRAW_CASH
            ),
            WalletType.AGENT, Set.of(
                    PermissionType.DEPOSIT_CASH,
                    PermissionType.WITHDRAW_CASH,
                    PermissionType.SEND_MONEY,
                    PermissionType.RECEIVE_MONEY
            )
    );

    private static final Map<WalletType, BigDecimal> DEFAULT_LIMITS = Map.of(
            WalletType.USER, new BigDecimal("500000"),
            WalletType.MERCHANT, new BigDecimal("5000000"),
            WalletType.AGENT, new BigDecimal("10000000")
    );


    @Override
    public boolean canPerformOperation(Wallet wallet, PermissionType operation, BigDecimal amount) {
        // 1. Vérifier le statut du wallet
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            return false;
        }

        // 2. Chercher une permission personnalisée
        Optional<WalletPermission> customPermission =
                permissionRepository.findByWalletAndPermissionType(wallet, operation);

        if (customPermission.isPresent()) {
            // Utiliser la permission personnalisée
            return validatePermission(customPermission.get(), amount);
        }

        // 3. Utiliser les permissions par défaut
        Set<PermissionType> defaultPerms = DEFAULT_PERMISSIONS.get(wallet.getType());
        if (defaultPerms == null || !defaultPerms.contains(operation)) {
            return false;
        }

        // 4. Vérifier la limite par défaut
        BigDecimal defaultLimit = DEFAULT_LIMITS.get(wallet.getType());
        return amount.compareTo(defaultLimit) <= 0;
    }

    @Override
    public void grantPermission(Long walletId, PermissionType type, BigDecimal maxAmount, BigDecimal dailyLimit) {


        Wallet wallet = walletRepository.findById(walletId).orElseThrow(
                () -> new WalletNotFoundException("Wallet with id: " + walletId));

        WalletPermission permission = new WalletPermission();
        permission.setWallet(wallet);
        permission.setPermissionType(type);
        permission.setEnabled(true);
        permission.setMaxAmount(maxAmount);
        permission.setDailyLimit(dailyLimit);
        permission.setCreatedAt(LocalDateTime.now());

        permissionRepository.save(permission);
    }

    @Override
    public void revokePermission(Wallet wallet, PermissionType type) {

        permissionRepository.findByWalletAndPermissionType(wallet, type)
                .ifPresent(p -> {
                    p.setEnabled(false);
                    p.setUpdatedAt(LocalDateTime.now());
                    permissionRepository.save(p);
                });

    }
    
    @Override
    public void revokePermissionById(Long permissionId) {
        log.info("[PERMISSION_REVOKE] Revoking permission with ID: {}", permissionId);
        
        WalletPermission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission with ID " + permissionId + " not found"));
        
        permission.setEnabled(false);
        permission.setUpdatedAt(LocalDateTime.now());
        permissionRepository.save(permission);
        
        log.info("[PERMISSION_REVOKE_SUCCESS] Revoked permission ID: {}", permissionId);
    }

    @Override
    public void updatePermissionLimit(Wallet wallet, PermissionType type, BigDecimal newLimit) {
        permissionRepository.findByWalletAndPermissionType(wallet, type)
                .ifPresent(p -> {
                    p.setMaxAmount(newLimit);
                    p.setUpdatedAt(LocalDateTime.now());
                    permissionRepository.save(p);
                });
    }

    @Override
    public void initializeDefaultPermissions(Long walletId) {

        Wallet wallet = walletRepository.findById(walletId).orElseThrow(
                () -> new WalletNotFoundException("Wallet with id: " + walletId)
        );

        Set<PermissionType> defaultPerms = DEFAULT_PERMISSIONS.get(wallet.getType());
        BigDecimal defaultLimit = DEFAULT_LIMITS.get(wallet.getType());

        if (defaultPerms != null) {
            for (PermissionType permType : defaultPerms) {
                WalletPermission permission = new WalletPermission();
                permission.setWallet(wallet);
                permission.setPermissionType(permType);
                permission.setEnabled(true);
                permission.setMaxAmount(defaultLimit);
                permission.setDailyLimit(defaultLimit.multiply(new BigDecimal("3"))); // 3x limite
                permission.setCreatedAt(LocalDateTime.now());

                permissionRepository.save(permission);
            }
        }
    }

    @Override
    public void customizePermission(Wallet wallet, PermissionType type, BigDecimal maxAmount,
                                    BigDecimal dailyLimit, Boolean requiresApproval) {

        WalletPermission permission = permissionRepository
                .findByWalletAndPermissionType(wallet, type)
                .orElse(new WalletPermission());

        permission.setWallet(wallet);
        permission.setPermissionType(type);
        permission.setEnabled(true);
        permission.setMaxAmount(maxAmount);
        permission.setDailyLimit(dailyLimit);
        permission.setRequiresApproval(requiresApproval);
        permission.setUpdatedAt(LocalDateTime.now());

        permissionRepository.save(permission);
    }

    @Override
    public BigDecimal getTodayTotal(Wallet wallet, PermissionType operation) {
        return null;
    }

    @Override
    public WalletPermissionDTO updatePermission(Long permissionId, UpdatePermissionRequest request) {
        return null;
    }

    @Override
    public List<WalletPermissionDTO> getWalletPermissions(String walletNumber) {
        return List.of();
    }
    
    @Override
    public List<com.zaphira.wallet.dto.response.WalletPermissionResponse> getPermissionsByWalletId(Long walletId) {
        log.info("[PERMISSION_LIST] Fetching permissions for wallet ID: {}", walletId);
        
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet with ID " + walletId + " not found"));
        List<WalletPermission> permissions = permissionRepository.findByWallet(wallet);
        
        List<com.zaphira.wallet.dto.response.WalletPermissionResponse> responses = permissions.stream()
                .map(p -> com.zaphira.wallet.dto.response.WalletPermissionResponse.builder()
                        .id(p.getId())
                        .walletId(wallet.getId())
                        .walletNumber(wallet.getWalletNumber())
                        .permissionType(p.getPermissionType().name())
                        .maxAmount(p.getMaxAmount())
                        .dailyLimit(p.getDailyLimit())
                        .enabled(p.getEnabled())
                        .requiresApproval(p.getRequiresApproval())
                        .createdAt(p.getCreatedAt())
                        .updatedAt(p.getUpdatedAt())
                        .build())
                .toList();
        
        log.info("[PERMISSION_LIST_SUCCESS] Retrieved {} permissions", responses.size());
        return responses;
    }

    @Override
    public List<WalletDTO> getWalletsAccessibleByUser(Long userId) {
        return List.of();
    }

    @Override
    public WalletPermissionDTO getUserPermissionForWallet(Long userId, String walletNumber) {
        return null;
    }

    @Override
    public List<EffectivePermission> getEffectivePermissions(Wallet wallet) {

        List<EffectivePermission> effective = new ArrayList<>();

        // Permissions par défaut
        Set<PermissionType> defaultPerms = DEFAULT_PERMISSIONS.get(wallet.getType());
        BigDecimal defaultLimit = DEFAULT_LIMITS.get(wallet.getType());

        if (defaultPerms != null) {
            for (PermissionType type : defaultPerms) {
                // Chercher personnalisation
                Optional<WalletPermission> custom =
                        permissionRepository.findByWalletAndPermissionType(wallet, type);

                EffectivePermission ep = new EffectivePermission();
                ep.setType(type);
                ep.setEnabled(custom.map(WalletPermission::getEnabled).orElse(true));
                ep.setMaxAmount(custom.map(WalletPermission::getMaxAmount).orElse(defaultLimit));
                ep.setDailyLimit(custom.map(WalletPermission::getDailyLimit)
                        .orElse(defaultLimit.multiply(new BigDecimal("3"))));
                ep.setCustomized(custom.isPresent());

                effective.add(ep);
            }
        }

        return effective;
    }

    @Override
    public boolean hasPermission(Long userId, String walletNumber, String action) {
        return false;
    }

    @Override
    public boolean canTransact(Long userId, String walletNumber, BigDecimal amount) {
        return false;
    }

    @Override
    public boolean canManagePermissions(Long userId, String walletNumber) {
        return false;
    }

    @Override
    public boolean isOwner(Long userId, String walletNumber) {
        return false;
    }

    @Override
    public void propagatePermissionToSubWallets(Long permissionId, boolean recursive) {

    }

    //methode utilitaire

    private boolean validatePermission(WalletPermission permission, BigDecimal amount) {
        if (!permission.getEnabled()) {
            return false;
        }

        if (permission.getMaxAmount() != null &&
                amount.compareTo(permission.getMaxAmount()) > 0) {
            return false;
        }

        return true;
    }
}
