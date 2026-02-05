package com.zaphira.wallet.controller;

import com.zaphira.common.dto.WalletSummaryDTO;
import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.dto.TransferRequest;
import com.zaphira.wallet.dto.request.*;
import com.zaphira.wallet.dto.response.CreateWalletResponse;
import com.zaphira.wallet.dto.response.TransactionValidationResponse;
import com.zaphira.wallet.dto.response.WalletHistoryResponse;
import com.zaphira.wallet.dto.response.WalletStatementResponse;
import com.zaphira.wallet.dto.response.BalanceHistoryResponse;

import com.zaphira.wallet.service.WalletService;
import com.zaphira.wallet.service.WalletHistoryService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;


import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WalletHistoryService walletHistoryService;
   
//
//    /**
//     * Crée un wallet pour un utilisateur donné.
//     * @param userId ID de l'utilisateur
//     * @param currency Devise du wallet (XOF par défaut)
//     * @return WalletDTO avec l'ID et le numéro du wallet
//     */
    // LOT 1: Create Wallet - REGULAR + MERCHANT + ADMIN
    @PostMapping
    @PreAuthorize("hasAnyRole('REGULAR', 'MERCHANT', 'ADMIN')")
    public ResponseEntity<?> createWallet(@RequestBody CreateWalletRequest request) {
        // ✅ Passer directement le request complet
        CreateWalletResponse wallet = walletService.createWalletForUser(request);
        log.info("✅ Wallet created for user {}: {}", request.getUserId(), wallet.getWalletNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    // LOT 1: Create Merchant Wallet - MERCHANT only
    @PostMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> createMerchantWallet(@Validated @RequestBody CreateMerchantWalletRequest request) {
        CreateWalletResponse merchantWallet = walletService.createWalletForMerchant(request);
        log.info("Creating wallet for merchant {}: {}", request.getWalletNumber(), merchantWallet.getWalletNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(merchantWallet);
    }

    // Internal endpoint - used by transaction-service
    @PostMapping("/{walletNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity <WalletSummaryDTO> getWalletSummaryByWalletNumber(@PathVariable String walletNumber) {
        log.info("Fetching wallet summary for wallet {}", walletNumber);
        WalletSummaryDTO walletSummaryDTO = walletService.getWalletSummaryByWalletNumber(walletNumber);
        log.info("Fetching wallet id for wallet {}", walletSummaryDTO.getWalletId());
        return ResponseEntity.ok(walletSummaryDTO);
    }

    // LOT 1: Get Wallet Details - Owner or ADMIN
    @GetMapping("/{walletNumber}")
    @PreAuthorize("@walletSecurity.canView(#walletNumber)")
    public ResponseEntity<WalletDTO> getWallet(@PathVariable String walletNumber) {
        log.info("Fetching wallet: {}", walletNumber);
        WalletDTO wallet = walletService.getWalletByNumber(walletNumber);
        return ResponseEntity.ok(wallet);
    }

    // Get Wallet by ID - ADMIN only (internal ID exposure)
    @GetMapping("/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> getWalletById(@PathVariable Long id) {
        log.info("Fetching wallet by ID: {}", id);
        WalletDTO wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }

    // Get User Wallets - Owner or ADMIN
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal")
    public ResponseEntity<List<WalletDTO>> getUserWallets(@PathVariable Long userId) {
        log.info("Fetching wallets for user: {}", userId);
        List<WalletDTO> wallets = walletService.getUserWallets(userId);
        return ResponseEntity.ok(wallets);
    }


    // LOT 1: Get Wallet Summary - Owner or ADMIN
    @GetMapping("/user/{userId}/summary")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal")
    public ResponseEntity<WalletSummaryDTO> getWalletSummary(@PathVariable Long userId) {
        log.info("Fetching wallet summary for user: {}", userId);
        WalletSummaryDTO summary = walletService.getWalletSummary(userId);
        return ResponseEntity.ok(summary);
    }

    // ========== Gestion du statut (ADMIN only) ==========

    // LOT 1: Freeze Wallet - ADMIN only
    @PutMapping("/{walletNumber}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> freezeWallet(
            @PathVariable String walletNumber,
            @Valid @RequestBody FreezeWalletRequest request) {
        log.info("Freezing wallet: {}", walletNumber);
        WalletDTO wallet = walletService.freezeWallet(walletNumber, request);
        return ResponseEntity.ok(wallet);
    }

    // LOT 1: Unfreeze Wallet - ADMIN only
    @PutMapping("/{walletNumber}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> unfreezeWallet(
            @PathVariable String walletNumber,
            @RequestParam String unfrozenBy,
            @RequestParam(required = false) String notes) {
        log.info("Unfreezing wallet: {}", walletNumber);
        WalletDTO wallet = walletService.unfreezeWallet(walletNumber, unfrozenBy, notes);
        return ResponseEntity.ok(wallet);
    }

    // LOT 1: Suspend Wallet - ADMIN only
    @PutMapping("/{walletNumber}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> suspendWallet(
            @PathVariable String walletNumber,
            @RequestParam String reason,
            @RequestParam String suspendedBy) {
        log.info("Suspending wallet: {}", walletNumber);
        WalletDTO wallet = walletService.suspendWallet(walletNumber, reason, suspendedBy);
        return ResponseEntity.ok(wallet);
    }

    // LOT 1: Activate Wallet - ADMIN only
    @PutMapping("/{walletNumber}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> activateWallet(
            @PathVariable String walletNumber,
            @RequestParam String activatedBy) {
        log.info("Activating wallet: {}", walletNumber);
        WalletDTO wallet = walletService.activateWallet(walletNumber, activatedBy);
        return ResponseEntity.ok(wallet);
    }

    // LOT 1: Close Wallet - ADMIN only
    @PutMapping("/{walletNumber}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> closeWallet(
            @PathVariable String walletNumber,
            @RequestParam String closedBy,
            @RequestParam String reason) {
        log.info("Closing wallet: {}", walletNumber);
        WalletDTO wallet = walletService.closeWallet(walletNumber, closedBy, reason);
        return ResponseEntity.ok(wallet);
    }

    // ========== Gestion des soldes (Internal - ADMIN only) ==========

    // Internal: Credit Wallet - ADMIN only
    @PostMapping("/{walletId}/credit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> creditWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequest request) {
        log.info("Crediting wallet: {} with amount: {}", walletId, request.getAmount());
        WalletDTO wallet = walletService.creditWallet(walletId, request);
        return ResponseEntity.ok(wallet);
    }

    // Internal: Debit Wallet - ADMIN only
    @PostMapping("/{walletId}/debit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> debitWallet(
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequest request) {
        log.info("Debiting wallet: {} with amount: {}", walletId, request.getAmount());
        WalletDTO wallet = walletService.debitWallet(walletId, request);
        return ResponseEntity.ok(wallet);
    }

    // Internal: Block Amount - ADMIN only
    @PostMapping("/{walletId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> blockAmount(
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequest request) {
        log.info("Blocking amount in wallet: {}", walletId);
        WalletDTO wallet = walletService.blockAmount(walletId, request);
        return ResponseEntity.ok(wallet);
    }

    // Internal: Unblock Amount - ADMIN only
    @PostMapping("/{walletId}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> unblockAmount(
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequest request) {
        log.info("Unblocking amount in wallet: {}", walletId);
        WalletDTO wallet = walletService.unblockAmount(walletId, request);
        return ResponseEntity.ok(wallet);
    }

    // Internal: Release Blocked Amount - ADMIN only
    @PostMapping("/{walletId}/release-blocked")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> releaseBlockedAmount(
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequest request) {
        log.info("Releasing blocked amount from wallet: {}", walletId);
        WalletDTO wallet = walletService.releaseBlockedAmount(walletId, request);
        return ResponseEntity.ok(wallet);
    }


    // ========== Validation de transaction (Internal) ==========

    // Internal: Validate Transaction - Used by transaction-service
    @PostMapping("/validate-transaction")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionValidationResponse> validateTransaction(
            @Valid @RequestBody TransactionValidationRequest request) {
        log.info("Validating transaction for wallet: {}", request.getWalletNumber());
        TransactionValidationResponse response = walletService.validateTransaction(request);
        return ResponseEntity.ok(response);
    }

    // ========== Gestion des limites ==========

    // LOT 3: Update Wallet Limits - ADMIN only (can force limits on any wallet)
    @PutMapping("/{walletNumber}/limits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDTO> updateLimits(
            @PathVariable String walletNumber,
            @RequestParam(required = false) BigDecimal dailyLimit,
            @RequestParam(required = false) BigDecimal monthlyLimit) {
        log.info("Updating limits for wallet: {}", walletNumber);
        WalletDTO wallet = walletService.updateLimits(walletNumber, dailyLimit, monthlyLimit);
        return ResponseEntity.ok(wallet);
    }

    // LOT 3: Get Wallet Limits - Owner or ADMIN
    

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

    // ========== LOT 5: History & Statements ==========

    /**
     * LOT 5: Get wallet transaction history with pagination
     * Accessible by wallet owner or ADMIN
     */
    @GetMapping("/{walletNumber}/history")
    @PreAuthorize("@walletSecurity.canView(#walletNumber)")
    public ResponseEntity<WalletHistoryResponse> getWalletHistory(
            @PathVariable String walletNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Fetching history for wallet: {} (page: {}, size: {})", walletNumber, page, size);
        
        LocalDateTime start = startDate != null ? 
                LocalDateTime.parse(startDate, DateTimeFormatter.ISO_DATE_TIME) : null;
        LocalDateTime end = endDate != null ? 
                LocalDateTime.parse(endDate, DateTimeFormatter.ISO_DATE_TIME) : null;
        
        WalletHistoryResponse history = walletHistoryService.getWalletHistory(
                walletNumber, page, size, start, end);
        return ResponseEntity.ok(history);
    }

    /**
     * LOT 5: Generate account statement for a period
     * Accessible by wallet owner or ADMIN
     */
    @GetMapping("/{walletNumber}/statement")
    @PreAuthorize("@walletSecurity.canView(#walletNumber)")
    public ResponseEntity<WalletStatementResponse> generateStatement(
            @PathVariable String walletNumber,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("Generating statement for wallet: {} from {} to {}", walletNumber, startDate, endDate);
        
        LocalDateTime start = LocalDateTime.parse(startDate, DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime end = LocalDateTime.parse(endDate, DateTimeFormatter.ISO_DATE_TIME);
        
        WalletStatementResponse statement = walletHistoryService.generateStatement(
                walletNumber, start, end);
        return ResponseEntity.ok(statement);
    }

    /**
     * LOT 5: Download statement as PDF
     * Accessible by wallet owner or ADMIN
     */
    @GetMapping("/{walletNumber}/statement/download")
    @PreAuthorize("@walletSecurity.canView(#walletNumber)")
    public ResponseEntity<Resource> downloadStatement(
            @PathVariable String walletNumber,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("Downloading statement for wallet: {}", walletNumber);
        
        LocalDateTime start = LocalDateTime.parse(startDate, DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime end = LocalDateTime.parse(endDate, DateTimeFormatter.ISO_DATE_TIME);
        
        Resource pdfResource = walletHistoryService.downloadStatementPdf(
                walletNumber, start, end);
        
        String filename = "statement_" + walletNumber + "_" + 
                startDate.replace(":", "-") + "_to_" + endDate.replace(":", "-") + ".pdf";
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfResource);
    }

    /**
     * LOT 5: Get balance history over time (for charts/analytics)
     * Accessible by wallet owner or ADMIN
     */
    @GetMapping("/{walletNumber}/balance-history")
    @PreAuthorize("@walletSecurity.canView(#walletNumber)")
    public ResponseEntity<BalanceHistoryResponse> getBalanceHistory(
            @PathVariable String walletNumber,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("Fetching balance history for wallet: {} from {} to {}", walletNumber, startDate, endDate);
        
        LocalDateTime start = LocalDateTime.parse(startDate, DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime end = LocalDateTime.parse(endDate, DateTimeFormatter.ISO_DATE_TIME);
        
        BalanceHistoryResponse balanceHistory = walletHistoryService.getBalanceHistory(
                walletNumber, start, end);
        return ResponseEntity.ok(balanceHistory);
    }

    // ========== Commented Endpoints (Legacy) ==========
    
    /**
     * Récupère le wallet d'un utilisateur via son ID.
     */
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<?> getWalletByUserId(@PathVariable Long userId) {
//        try {

    /**
     * Transfert d'argent entre deux wallets (Lot 2).
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('USER','MERCHANT','ADMIN')")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        try {
            walletService.transfer(request);
            log.info("Transfer successful from {} to {} amount {}", request.getSenderWalletNumber(), request.getReceiverWalletNumber(), request.getAmount());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Transfer failed from {} to {}: {}", request.getSenderWalletNumber(), request.getReceiverWalletNumber(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}