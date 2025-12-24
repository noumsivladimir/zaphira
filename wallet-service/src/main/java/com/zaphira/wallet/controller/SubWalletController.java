package com.zaphira.wallet.controller;


import com.zaphira.wallet.dto.request.CreateSubWalletRequest;
import com.zaphira.wallet.models.entities.SubWallet;
import com.zaphira.wallet.service.WalletHierarchyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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


    @PostMapping("/create")
    public ResponseEntity<?> createSubWallet(@RequestBody CreateSubWalletRequest request){


        try {

            SubWallet subWallet= walletHierarchyService.createSubWallet(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(subWallet);


        } catch (Exception e) {
            log.error("Failed to create subWallet: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating wallet: " + e.getMessage());
        }


    }
//
//    @PostMapping
//    public ResponseEntity<?> createWallet(@RequestBody CreateWalletRequest request) {
//        try {
//            // ✅ Passer directement le request complet
//            CreateWalletResponse wallet = walletService.createWalletForUser(request);
//
//            log.info("✅ Wallet created for user {}: {}", request.getUserId(), wallet.getWalletNumber());
//            return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
//        } catch (Exception e) {
//            log.error("❌ Failed to create wallet for user {}: {}", request.getUserId(), e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error creating wallet: " + e.getMessage());
//        }
//    }
}
