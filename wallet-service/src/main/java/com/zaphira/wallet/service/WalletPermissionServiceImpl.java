package com.zaphira.wallet.service;

import com.zaphira.common.model.enums.PermissionType;
import com.zaphira.wallet.dto.EffectivePermission;
import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.dto.WalletPermissionDTO;
import com.zaphira.wallet.dto.request.GrantPermissionRequest;
import com.zaphira.wallet.dto.request.UpdatePermissionRequest;
import com.zaphira.wallet.exception.WalletNotFoundException;
import com.zaphira.wallet.models.entities.Wallet;
import com.zaphira.wallet.models.entities.WalletPermission;
import com.zaphira.wallet.models.enums.WalletStatus;
import com.zaphira.wallet.models.enums.WalletType;
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

    public WalletPermissionServiceImpl(WalletPermissionRepository permissionRepository, WalletRepository walletRepository) {
        this.permissionRepository = permissionRepository;
        this.walletRepository = walletRepository;
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

//    public void grantPermission(Long walletId, PermissionType type) {
//
//        Wallet wallet = walletRepository.findById(walletId).orElseThrow(
//                () -> new WalletNotFoundException("Wallet with id: " + walletId));
//
//        WalletPermission permission = new WalletPermission();
//        permission.setWallet(wallet);
//        permission.setPermissionType(type);
//        permission.setEnabled(true);
//        permission.setCreatedAt(LocalDateTime.now());
//        permissionRepository.save(permission);
//    }


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
    public WalletPermissionDTO grantPermission(GrantPermissionRequest request) {

        Wallet wallet = walletRepository.findById(request.getWalletId()).orElseThrow(
                () -> new WalletNotFoundException("Wallet with id: " + request.getWalletId())
        );

        // Vérifier si la permission existe déjà
        Optional<WalletPermission> existingPermission =
                permissionRepository.findByWalletIdAndPermissionType(wallet.getId(), request.getPermissionType());

        if (existingPermission.isPresent()) {

            WalletPermission permission = existingPermission.get();

            if (permission.getEnabled() != true){

                permission.setEnabled(true);
                permissionRepository.save(permission);
                log.info("Re-enabled permission {} for wallet {}", permission.getPermissionType(), wallet.getWalletNumber());
                return WalletPermissionDTO.builder()
                        .walletId(request.getWalletId())
                        .walletPermission(permissionRepository.findPermissionTypesByWalletId(request.getWalletId()))
                        .build();
            } else {
                log.error("Permission {} already enabled  for wallet {}", permission.getPermissionType(), request.getWalletId());
            }

        }


        WalletPermission permission = existingPermission.get();

        permission.setWallet(wallet);
        permission.setPermissionType(request.getPermissionType());
        permission.setEnabled(true);
        permission.setMaxAmount(request.getMaxAmount());
        permission.setDailyLimit(request.getDailyLimit());
        permission.setRequiresApproval(request.getRequiresApproval());
        permission.setUpdatedAt(LocalDateTime.now());

        permissionRepository.save(permission);

        return WalletPermissionDTO.builder()
                .walletId(request.getWalletId())
                .walletNumber(wallet.getWalletNumber())
                .walletPermission(permissionRepository.findPermissionTypesByWalletId(request.getWalletId()))
                .build();
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
    public WalletPermissionDTO getWalletPermissions(String walletNumber) {

        Wallet wallet = walletRepository.findByWalletNumber(walletNumber).orElseThrow(
                () -> new WalletNotFoundException("Wallet with id: " + walletNumber)
        );

        WalletPermissionDTO dtos = WalletPermissionDTO.builder()
                .walletId(wallet.getId())
                .walletNumber(walletNumber)
                .walletPermission(permissionRepository.findPermissionTypesByWalletId(wallet.getId()))
                .build();

        return dtos;
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
    public List<PermissionType> getPermissionTypes(Long walletId) {
        return permissionRepository.findPermissionTypesByWalletId(walletId);
    }


    @Override
//    @Cacheable(value = "walletPermissions", key = "#walletId + '-' + #permissionType")
    public boolean hasPermission(Long walletId, PermissionType permissionType) {
        log.debug("Checking if wallet {} has permission {}", walletId, permissionType);

        // Vérifier que le wallet existe
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        // Vérifier la permission (requête DB optimisée)
        return permissionRepository.hasActivePermission(walletId, permissionType);
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