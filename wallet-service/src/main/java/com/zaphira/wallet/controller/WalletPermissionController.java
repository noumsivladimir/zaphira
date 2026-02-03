package com.zaphira.wallet.controller;


import com.zaphira.wallet.models.entities.WalletPermission;

import com.zaphira.wallet.service.WalletPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet/permission")
@RequiredArgsConstructor
@Slf4j
public class WalletPermissionController {
    private final WalletPermissionService walletPermissionService;
    

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
}
