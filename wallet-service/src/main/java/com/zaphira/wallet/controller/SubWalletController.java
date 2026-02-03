package com.zaphira.wallet.controller;


import com.zaphira.wallet.dto.request.CreateSubWalletRequest;
import com.zaphira.wallet.dto.response.SubWalletResponse;
import com.zaphira.wallet.service.WalletHierarchyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet/subWallet")
@Slf4j
@RequiredArgsConstructor
public class SubWalletController {

    private final WalletHierarchyService walletHierarchyService;


    // LOT 4: Create SubWallet - MERCHANT only (merchants create sub-wallets for business units)
    @PostMapping("/create")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> createSubWallet(@RequestBody CreateSubWalletRequest request){


        try {

            SubWalletResponse subWallet= walletHierarchyService.createSubWallet(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(subWallet);


        } catch (Exception e) {
            log.error("Failed to create subWallet");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating wallet: " + e.getMessage());
        }


    }
}
