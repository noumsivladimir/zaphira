package com.zaphira.wallet.controller;

import com.zaphira.common.dto.WalletSummaryDTO;
import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.dto.request.*;
import com.zaphira.wallet.dto.response.CreateWalletResponse;
import com.zaphira.wallet.dto.response.TransactionValidationResponse;
import com.zaphira.wallet.service.WalletQueryService;
import com.zaphira.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WalletQueryService walletQueryService;
//
//    /**
//     * Crée un wallet pour un utilisateur donné.
//     * @param userId ID de l'utilisateur
//     * @param currency Devise du wallet (XOF par défaut)
//     * @return WalletDTO avec l'ID et le numéro du wallet
//     */
    @PostMapping
    public ResponseEntity<?> createWallet(@RequestBody CreateWalletRequest request) {
        try {
            // ✅ Passer directement le request complet
            CreateWalletResponse wallet = walletService.createWalletForUser(request);

            log.info("✅ Wallet created for user {}: {}", request.getUserId(), wallet.getWalletNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
        } catch (Exception e) {
            log.error("❌ Failed to create wallet for user {}: {}", request.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating wallet: " + e.getMessage());
        }
    }

    @PostMapping("/merchant")
    public ResponseEntity<?> createMerchantWallet(@Validated @RequestBody CreateMerchantWalletRequest request) {
        try {
            CreateWalletResponse merchantWallet = walletService.createWalletForMerchant(request);
            log.info("Creating wallet for merchant {}: {}", request.getWalletNumber(), merchantWallet.getWalletNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(merchantWallet);
        } catch (Exception e) {
            log.error("Failed to create wallet for merchant {}: {}", request.getMerchantName(), e.getMessage(), e);
            return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating wallet: " + e.getMessage());
        }
    }

    @PostMapping("/{walletNumber}")
    public ResponseEntity <WalletSummaryDTO> getWalletSummaryByWalletNumber(@PathVariable String walletNumber) {
        log.info("Fetching wallet summary for wallet {}", walletNumber);
        WalletSummaryDTO walletSummaryDTO = walletService.getWalletSummaryByWalletNumber(walletNumber);
        log.info("Fetching wallet id for wallet {}", walletSummaryDTO.getWalletId());
        return ResponseEntity.ok(walletSummaryDTO);
    }





    @GetMapping("/{walletNumber}")
    public ResponseEntity<WalletDTO> getWallet(@PathVariable String walletNumber) {
        log.info("Fetching wallet: {}", walletNumber);
        WalletDTO wallet = walletService.getWalletByNumber(walletNumber);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<WalletDTO> getWalletById(@PathVariable Long id) {
        log.info("Fetching wallet by ID: {}", id);
        WalletDTO wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }

//    @GetMapping("/user/{userId}")
//    public ResponseEntity<List<WalletDTO>> getUserWallets(@PathVariable Long userId) {
//        log.info("Fetching wallets for user: {}", userId);
//        List<WalletDTO> wallets = walletQueryService.getUserWallets(userId);
//        return ResponseEntity.ok(wallets);
//    }


    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<WalletSummaryDTO> getWalletSummary(@PathVariable Long userId) {
        log.info("Fetching wallet summary for user: {}", userId);
        WalletSummaryDTO summary = walletService.getWalletSummary(userId);
        return ResponseEntity.ok(summary);
    }

    // ========== Gestion du statut ==========

    @PutMapping("/{walletNumber}/freeze")
    public ResponseEntity<WalletDTO> freezeWallet(
            @PathVariable String walletNumber,
            @Valid @RequestBody FreezeWalletRequest request) {
        log.info("Freezing wallet: {}", walletNumber);
        WalletDTO wallet = walletService.freezeWallet(walletNumber, request);
        return ResponseEntity.ok(wallet);
    }

    @PutMapping("/{walletNumber}/unfreeze")
    public ResponseEntity<WalletDTO> unfreezeWallet(
            @PathVariable String walletNumber,
            @RequestParam String unfrozenBy,
            @RequestParam(required = false) String notes) {
        log.info("Unfreezing wallet: {}", walletNumber);
        WalletDTO wallet = walletService.unfreezeWallet(walletNumber, unfrozenBy, notes);
        return ResponseEntity.ok(wallet);
    }

    @PutMapping("/{walletNumber}/suspend")
    public ResponseEntity<WalletDTO> suspendWallet(
            @PathVariable String walletNumber,
            @RequestParam String reason,
            @RequestParam String suspendedBy) {
        log.info("Suspending wallet: {}", walletNumber);
        WalletDTO wallet = walletService.suspendWallet(walletNumber, reason, suspendedBy);
        return ResponseEntity.ok(wallet);
    }

    @PutMapping("/{walletNumber}/activate")
    public ResponseEntity<WalletDTO> activateWallet(
            @PathVariable String walletNumber,
            @RequestParam String activatedBy) {
        log.info("Activating wallet: {}", walletNumber);
        WalletDTO wallet = walletService.activateWallet(walletNumber, activatedBy);
        return ResponseEntity.ok(wallet);
    }

    @PutMapping("/{walletNumber}/close")
    public ResponseEntity<WalletDTO> closeWallet(
            @PathVariable String walletNumber,
            @RequestParam String closedBy,
            @RequestParam String reason) {
        log.info("Closing wallet: {}", walletNumber);
        WalletDTO wallet = walletService.closeWallet(walletNumber, closedBy, reason);
        return ResponseEntity.ok(wallet);
    }

    // ========== Gestion des soldes ==========

    @PostMapping("/{walletId}/credit")
    public ResponseEntity<WalletDTO> creditWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequest request) {
        log.info("Crediting wallet: {} with amount: {}", walletId, request.getAmount());
        WalletDTO wallet = walletService.creditWallet(walletId, request);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{walletId}/debit")
    public ResponseEntity<WalletDTO> debitWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequest request) {
        log.info("Debiting wallet: {} with amount: {}", walletId, request.getAmount());
        WalletDTO wallet = walletService.debitWallet(walletId, request);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{walletId}/block")
    public ResponseEntity<WalletDTO> blockAmount(
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequest request) {
        log.info("Blocking amount in wallet: {}", walletId);
        WalletDTO wallet = walletService.blockAmount(walletId, request);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{walletId}/unblock")
    public ResponseEntity<WalletDTO> unblockAmount(
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequest request) {
        log.info("Unblocking amount in wallet: {}", walletId);
        WalletDTO wallet = walletService.unblockAmount(walletId, request);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{walletId}/release-blocked")
    public ResponseEntity<WalletDTO> releaseBlockedAmount(
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequest request) {
        log.info("Releasing blocked amount from wallet: {}", walletId);
        WalletDTO wallet = walletService.releaseBlockedAmount(walletId, request);
        return ResponseEntity.ok(wallet);
    }


    // ========== Validation de transaction ==========

    @PostMapping("/validate-transaction")
    public ResponseEntity<TransactionValidationResponse> validateTransaction(
            @Valid @RequestBody TransactionValidationRequest request) {
        log.info("Validating transaction for wallet: {}", request.getWalletNumber());
        TransactionValidationResponse response = walletService.validateTransaction(request);
        return ResponseEntity.ok(response);
    }

    // ========== Gestion des limites ==========

    @PutMapping("/{walletNumber}/limits")
    public ResponseEntity<WalletDTO> updateLimits(
            @PathVariable String walletNumber,
            @RequestParam(required = false) BigDecimal dailyLimit,
            @RequestParam(required = false) BigDecimal monthlyLimit) {
        log.info("Updating limits for wallet: {}", walletNumber);
        WalletDTO wallet = walletService.updateLimits(walletNumber, dailyLimit, monthlyLimit);
        return ResponseEntity.ok(wallet);
    }

    // ========== Utilitaires ==========
    @GetMapping("/{walletNumber}/has-balance")
    public ResponseEntity<Boolean> hasAvailableBalance(
            @PathVariable String walletNumber,
            @RequestParam BigDecimal amount) {
        boolean hasBalance = walletService.hasAvailableBalance(walletNumber, amount);
        return ResponseEntity.ok(hasBalance);
    }

    @PostMapping("/{walletNumber}/recalculate-balance")
    public ResponseEntity<Void> recalculateBalance(@PathVariable String walletNumber) {
        log.info("Recalculating balance for wallet: {}", walletNumber);
        walletService.recalculateBalance(walletNumber);
        return ResponseEntity.ok().build();
    }
    /**
     * Récupère le wallet d'un utilisateur via son ID.
     */
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<?> getWalletByUserId(@PathVariable Long userId) {
//        try {
//            WalletDTO wallet = walletService.getWalletByUserId(userId);
//            if (wallet == null) {
//                log.warn("⚠️ Wallet not found for user {}", userId);
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body("Wallet not found for user " + userId);
//            }
//            return ResponseEntity.ok(wallet);
//        } catch (Exception e) {
//            log.error("❌ Error fetching wallet for user {}: {}", userId, e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error fetching wallet: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Récupère un wallet via son numéro.
//     */
//    @GetMapping("/{walletNumber}")
//    public ResponseEntity<?> getWalletByNumber(@PathVariable String walletNumber) {
//        try {
//            WalletDTO wallet = walletService.getWalletByNumber(walletNumber);
//            if (wallet == null) {
//                log.warn("⚠️ Wallet not found with number {}", walletNumber);
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body("Wallet not found with number " + walletNumber);
//            }
//            return ResponseEntity.ok(wallet);
//        } catch (Exception e) {
//            log.error("❌ Error fetching wallet with number {}: {}", walletNumber, e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error fetching wallet: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Transfert d'argent entre deux wallets.
//     */
//    @PostMapping("/transfer")
//    public ResponseEntity<?> transfer(@RequestBody TransferRequest request) {
//        try {
//            walletService.transfer(
//                    request.getSenderWalletNumber(),
//                    request.getReceiverWalletNumber(),
//                    request.getAmount()
//            );
//            log.info("✅ Transfer successful from {} to {} amount {}",
//                    request.getSenderWalletNumber(),
//                    request.getReceiverWalletNumber(),
//                    request.getAmount());
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            log.error("❌ Transfer failed from {} to {}: {}",
//                    request.getSenderWalletNumber(),
//                    request.getReceiverWalletNumber(),
//                    e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body("Transfer failed: " + e.getMessage());
//        }
//    }
}