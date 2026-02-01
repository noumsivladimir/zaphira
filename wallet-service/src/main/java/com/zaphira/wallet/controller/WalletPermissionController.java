package com.zaphira.wallet.controller;


import com.zaphira.common.model.enums.PermissionType;
import com.zaphira.wallet.dto.WalletPermissionDTO;
import com.zaphira.wallet.dto.request.GrantPermissionRequest;
import com.zaphira.common.dto.response.PermissionCheckResponse;
import com.zaphira.wallet.models.entities.Wallet;
import com.zaphira.wallet.models.entities.WalletPermission;
import com.zaphira.wallet.repository.WalletPermissionRepository;
import com.zaphira.wallet.repository.WalletRepository;
import com.zaphira.wallet.service.WalletPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet/permission")
@RequiredArgsConstructor
@Slf4j
public class WalletPermissionController {
    private final WalletPermissionService walletPermissionService;
    private final WalletPermissionRepository walletPermissionRepository;
    private final WalletRepository walletRepository;

    @PostMapping("/id/{id}")
    public ResponseEntity<WalletPermission> addWalletPermission(@PathVariable Long id  ) {

        try {

            log.info("Adding permissions for wallet {}", id);

            walletPermissionService.initializeDefaultPermissions(id);

            WalletPermission walletDTO = new WalletPermission();
            return ResponseEntity.status(HttpStatus.CREATED).body(walletDTO);


        } catch (Exception e) {
            log.error("Error adding permissions for wallet {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }


    }

    @PostMapping("/grantPermission")
    public ResponseEntity<WalletPermissionDTO> grantPermission(@Validated @RequestBody GrantPermissionRequest request) {

        try {
            log.info("Adding {} permission for wallet {}", request.getPermissionType(), request.getWalletId());
            walletPermissionService.grantPermission(request);

            Wallet wallet = walletRepository.findById(request.getWalletId()).get();

            WalletPermissionDTO walletPermissionDTO = walletPermissionService.grantPermission(request);

            log.info("{} permissions for wallet {}", request.getPermissionType(), wallet.getWalletNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(walletPermissionDTO);
        } catch ( Exception e ) {
            log.error("Error adding permission for wallet {}", request.getWalletId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

//    @GetMapping("/wallet/{walletId}")
//    public ResponseEntity<?> getWalletPermissions(@PathVariable Long walletId) {
//        try {
//            log.info("Fetching permissions for wallet {}", walletId);
//            Wallet wallet = walletRepository.findById(walletId).orElseThrow(
//                    () -> new WalletNotFoundException("Wallet with id " + walletId + " not found")
//            );
//
//            WalletPermissionDTO permissions =
//                    walletPermissionService.getWalletPermissions(wallet.getWalletNumber());
//
//            return ResponseEntity.ok(Map.of(
//                    "walletId", walletId,
//                    "permissions", permissions
//            ));
//
//        } catch (WalletNotFoundException e) {
//            log.error("Wallet not found: {}", walletId, e);
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(Map.of("error", e.getMessage()));
//
//        } catch (Exception e) {
//            log.error("Error fetching permissions for wallet {}", walletId, e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "Failed to fetch permissions"));
//        }
//    }

    @GetMapping("/{walletId}")
    public ResponseEntity <?> getWalletPermissions (@PathVariable Long walletId) {

        try {
            log.info("Getting permissions for wallet {}", walletId);

            List<PermissionType> permissionTypes = walletPermissionService.getPermissionTypes(walletId);

            return ResponseEntity.status(HttpStatus.OK).body(permissionTypes);
        } catch (Exception e){
            log.error("Error getting permissions for wallet {}", walletId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        }
    }

    @GetMapping("/{walletId}/has-permission")
    public ResponseEntity<PermissionCheckResponse> hasPermission(
            @PathVariable Long walletId,
            @RequestParam PermissionType permissionType) {

        log.info("Checking if wallet {} has permission: {}", walletId, permissionType);

        boolean hasPermission = walletPermissionService.hasPermission(walletId, permissionType);

        PermissionCheckResponse response = PermissionCheckResponse.builder()
                .walletId(walletId)
                .permissionType(permissionType)
                .hasPermission(hasPermission)
                .build();

        return ResponseEntity.ok(response);
    }

}
