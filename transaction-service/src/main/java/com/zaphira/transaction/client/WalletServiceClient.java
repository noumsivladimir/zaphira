package com.zaphira.transaction.client;

import com.zaphira.common.dto.WalletDTO;
import com.zaphira.common.dto.WalletSummaryDTO;
import com.zaphira.common.dto.request.BalanceOperationRequest;
import com.zaphira.common.dto.response.PermissionCheckResponse;
import com.zaphira.common.model.enums.PermissionType;
import com.zaphira.transaction.dto.requests.TransactionValidationRequest;
import com.zaphira.transaction.dto.response.TransactionValidationResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "wallet-service",
        url = "${wallet.service.url}",
        path = "/api/wallet",
        fallbackFactory = WalletServiceClientFallbackFactory.class
)
public interface WalletServiceClient {


    /**
     /**
     * Récupère l'identifiant interne d'un wallet à partir de son numéro.
     */
    @PostMapping("/{walletNumber}")
    WalletSummaryDTO getWalletSummaryByWalletNumber(@PathVariable("walletNumber") String walletNumber);

    /**
     * Valider une transaction
     */
    @PostMapping("/validate-transaction")
    @CircuitBreaker(name = "walletService")
    @Retry(name = "walletService")
    TransactionValidationResponse validateTransaction(@Valid @RequestBody TransactionValidationRequest request);

    /**
     * Bloquer un montant
     */
    @PostMapping("/{walletId}/block")
    @CircuitBreaker(name = "walletService")
    void blockAmount(
            @PathVariable("walletId") Long walletId,
            @Valid @RequestBody BalanceOperationRequest request);

    /**
     * Débloquer un montant
     */
    @PostMapping("/{walletId}/unblock")
    @CircuitBreaker(name = "walletService")
    void unblockAmount(
            @PathVariable("walletId") Long walletId,
            @Valid @RequestBody BalanceOperationRequest request);

    /**
     * Libérer un montant bloqué (débit effectif)
     */
    @PostMapping("/{walletId}/release-blocked")
    @CircuitBreaker(name = "walletService")
    void releaseBlockedAmount(
            @PathVariable("walletId") Long walletId,
            @Valid @RequestBody BalanceOperationRequest request);

    /**
     * Créditer un wallet
     */
    @PostMapping("/{walletId}/credit")
    @CircuitBreaker(name = "walletService")
    void creditWallet(
            @PathVariable("walletId") Long walletId,
            @Valid @RequestBody BalanceOperationRequest request);

    /**
     * Débiter un wallet
     */
    @PostMapping("/{walletId}/debit")
    @CircuitBreaker(name = "walletService")
    void debitWallet(
            @PathVariable("walletId") Long walletId,
            @Valid @RequestBody BalanceOperationRequest request);

    /**
     * Obtenir le solde d'un wallet
     */
    @GetMapping("/{walletId}")
    @CircuitBreaker(name = "walletService")
    WalletDTO getWallet(@PathVariable("walletId") Long walletId);

    /**
     * Checker les Permissions d'un Wallet Id
     */
    @GetMapping("/permission/{walletId}")
    @CircuitBreaker(name = "walletService")
    List<PermissionType> getPermissionTypes(@PathVariable("walletId") Long walletId);


    /**
     * Verifier que un wallet a une permissiond définie
     * @Param: WalletID
     */
    @GetMapping("/permission/{walletId}/has-permission")
    PermissionCheckResponse hasPermission(@PathVariable("walletId") Long walletId,
                                          @RequestParam PermissionType permissionType);

}