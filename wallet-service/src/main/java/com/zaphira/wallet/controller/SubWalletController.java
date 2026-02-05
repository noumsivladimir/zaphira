package com.zaphira.wallet.controller;


import com.zaphira.wallet.dto.request.CreateSubWalletRequest;
import com.zaphira.wallet.dto.response.SubWalletResponse;
import com.zaphira.wallet.service.WalletHierarchyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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


        SubWalletResponse subWallet = walletHierarchyService.createSubWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(subWallet);


    }
    
    /**
     * Get sub-wallet details by ID.
     * 
     * GET /api/wallet/subWallet/{id}
     * 
     * Response:
     * {
     *   "id": 123,
     *   "subWalletName": "Business Account",
     *   "type": "BUSINESS",
     *   "accountNumber": "SW1234567890",
     *   "availableBalance": 1000.00,
     *   "totalBalance": 1000.00,
     *   "currency": "XAF",
     *   "status": "ACTIVE",
     *   "managingWallets": [...]
     * }
     * 
     * @param id Sub-wallet ID
     * @return Sub-wallet details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public ResponseEntity<?> getSubWalletById(@PathVariable Long id) {
        log.info("[SUBWALLET_GET_API] Fetching sub-wallet: {}", id);
        SubWalletResponse subWallet = walletHierarchyService.getSubWalletById(id);
        return ResponseEntity.ok(subWallet);
    }
    
    /**
     * Delete sub-wallet by ID.
     * 
     * DELETE /api/wallet/subWallet/{id}
     * 
     * Conditions:
     * - Sub-wallet balance must be zero
     * - All relationships will be removed
     * 
     * Response: 204 No Content on success
     * 
     * @param id Sub-wallet ID
     * @return No content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public ResponseEntity<?> deleteSubWallet(@PathVariable Long id) {
        log.info("[SUBWALLET_DELETE_API] Deleting sub-wallet: {}", id);
        walletHierarchyService.deleteSubWallet(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get list of sub-wallets for a parent wallet.
     * 
     * GET /api/wallet/subWallet/{parentWalletNumber}/list
     * 
     * Response: List of sub-wallets
     * [
     *   {
     *     "id": 123,
     *     "subWalletName": "Savings",
     *     "type": "SAVINGS",
     *     "availableBalance": 5000.00,
     *     ...
     *   }
     * ]
     * 
     * @param parentWalletNumber Parent wallet number
     * @return List of sub-wallets
     */
    @GetMapping("/{parentWalletNumber}/list")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN', 'REGULAR')")
    public ResponseEntity<?> listSubWallets(@PathVariable String parentWalletNumber) {
        log.info("[SUBWALLET_LIST_API] Fetching sub-wallets for wallet: {}", parentWalletNumber);
        List<SubWalletResponse> subWallets = walletHierarchyService.getSubWallets(parentWalletNumber);
        return ResponseEntity.ok(subWallets);
    }
}
