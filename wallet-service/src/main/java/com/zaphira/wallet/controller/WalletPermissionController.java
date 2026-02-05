package com.zaphira.wallet.controller;


import com.zaphira.wallet.dto.response.WalletPermissionResponse;
import com.zaphira.wallet.model.entities.WalletPermission;

import com.zaphira.wallet.service.WalletPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet/permission")
@RequiredArgsConstructor
@Slf4j
public class WalletPermissionController {
    private final WalletPermissionService walletPermissionService;
    

    @PostMapping("/id/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MERCHANT')")
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
    
    /**
     * Get all permissions for a wallet.
     * 
     * GET /api/wallet/permission/{walletId}
     * 
     * Response: List of permissions
     * [
     *   {
     *     "id": 1,
     *     "walletId": 123,
     *     "permissionType": "TRANSFER",
     *     "maxAmount": 1000000.00,
     *     "dailyLimit": 5000000.00,
     *     "enabled": true,
     *     "requiresApproval": false,
     *     "createdAt": "2026-02-04T10:00:00",
     *     "updatedAt": "2026-02-04T10:00:00"
     *   }
     * ]
     * 
     * @param walletId Wallet ID
     * @return List of permissions
     */
    @GetMapping("/{walletId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MERCHANT', 'REGULAR')")
    public ResponseEntity<?> getWalletPermissions(@PathVariable Long walletId) {
        try {
            log.info("[PERMISSION_GET_API] Fetching permissions for wallet: {}", walletId);
            List<WalletPermissionResponse> permissions = walletPermissionService.getPermissionsByWalletId(walletId);
            return ResponseEntity.ok(permissions);
        } catch (IllegalArgumentException e) {
            log.warn("[PERMISSION_GET_NOT_FOUND] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Wallet not found: " + e.getMessage());
        } catch (Exception e) {
            log.error("[PERMISSION_GET_ERROR] Failed to get permissions for wallet: {}", walletId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching permissions: " + e.getMessage());
        }
    }
    
    /**
     * Revoke a permission by ID.
     * 
     * DELETE /api/wallet/permission/{id}
     * 
     * Sets permission.enabled = false.
     * Does not physically delete the permission record.
     * 
     * Response: 204 No Content on success
     * 
     * @param id Permission ID
     * @return No content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MERCHANT')")
    public ResponseEntity<?> revokePermission(@PathVariable Long id) {
        try {
            log.info("[PERMISSION_REVOKE_API] Revoking permission: {}", id);
            walletPermissionService.revokePermissionById(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("[PERMISSION_REVOKE_NOT_FOUND] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Permission not found: " + e.getMessage());
        } catch (Exception e) {
            log.error("[PERMISSION_REVOKE_ERROR] Failed to revoke permission: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error revoking permission: " + e.getMessage());
        }
    }
}
