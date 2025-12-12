package com.zaphira.wallet.controller;

import com.zaphira.common.dto.WalletDTO;
import com.zaphira.wallet.dto.TransferRequest;
import com.zaphira.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * Crée un wallet pour un utilisateur donné.
     * @param userId ID de l'utilisateur
     * @param currency Devise du wallet (XOF par défaut)
     * @return WalletDTO avec l'ID et le numéro du wallet
     */
    @PostMapping
    public ResponseEntity<?> createWallet(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "XOF") String currency) {
        try {
            WalletDTO wallet = walletService.createWallet(userId);
            log.info("✅ Wallet created for user {}: {}", userId, wallet.getWalletNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
        } catch (Exception e) {
            log.error("❌ Failed to create wallet for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating wallet: " + e.getMessage());
        }
    }

    /**
     * Récupère le wallet d'un utilisateur via son ID.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getWalletByUserId(@PathVariable Long userId) {
        try {
            WalletDTO wallet = walletService.getWalletByUserId(userId);
            if (wallet == null) {
                log.warn("⚠️ Wallet not found for user {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Wallet not found for user " + userId);
            }
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            log.error("❌ Error fetching wallet for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching wallet: " + e.getMessage());
        }
    }

    /**
     * Récupère un wallet via son numéro.
     */
    @GetMapping("/{walletNumber}")
    public ResponseEntity<?> getWalletByNumber(@PathVariable String walletNumber) {
        try {
            WalletDTO wallet = walletService.getWalletByNumber(walletNumber);
            if (wallet == null) {
                log.warn("⚠️ Wallet not found with number {}", walletNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Wallet not found with number " + walletNumber);
            }
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            log.error("❌ Error fetching wallet with number {}: {}", walletNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching wallet: " + e.getMessage());
        }
    }

    /**
     * Transfert d'argent entre deux wallets.
     */
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest request) {
        try {
            walletService.transfer(
                    request.getSenderWalletNumber(),
                    request.getReceiverWalletNumber(),
                    request.getAmount()
            );
            log.info("✅ Transfer successful from {} to {} amount {}",
                    request.getSenderWalletNumber(),
                    request.getReceiverWalletNumber(),
                    request.getAmount());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Transfer failed from {} to {}: {}",
                    request.getSenderWalletNumber(),
                    request.getReceiverWalletNumber(),
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Transfer failed: " + e.getMessage());
        }
    }
}
