package com.zaphira.service_user.client;


import com.zaphira.service_user.dto.request.CreateWalletRequest;
import com.zaphira.service_user.dto.response.WalletResponse;
import com.zaphira.common.dto.request.FreezeWalletRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "wallet-service",
        url = "${wallet.service.url}",
        path = "/api/wallets",
        configuration = WalletServiceFeignConfig.class,
        fallbackFactory = WalletServiceClientFallbackFactory.class
)
public interface WalletServiceClient {

    /**
     * Créer un wallet pour un utilisateur
     */
    @PostMapping
    @CircuitBreaker(name = "walletService")
    @Retry(name = "walletService")
    WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request);

    /**
     * Créer automatiquement un wallet (endpoint simplifié)
     */
    @PostMapping("/auto-create/{userId}")
    @CircuitBreaker(name = "walletService")
    @Retry(name = "walletService")
    WalletResponse autoCreateWallet(@PathVariable("userId") Long userId);

    /**
     * Obtenir un wallet par numéro
     */
    @GetMapping("/{walletNumber}")
    @CircuitBreaker(name = "walletService")
    WalletResponse getWallet(@PathVariable("walletNumber") String walletNumber);

    /**
     * Obtenir tous les wallets d'un utilisateur
     */
    @GetMapping("/user/{userId}")
    @CircuitBreaker(name = "walletService")
    List<WalletResponse> getUserWallets(@PathVariable("userId") Long userId);

    /**
     * Obtenir le wallet principal d'un utilisateur
     */
    @GetMapping("/user/{userId}/primary")
    @CircuitBreaker(name = "walletService")
    WalletResponse getPrimaryWallet(@PathVariable("userId") Long userId);

    /**
     * Geler un wallet
     */
    @PutMapping("/{walletNumber}/freeze")
    @CircuitBreaker(name = "walletService")
    WalletResponse freezeWallet(
            @PathVariable("walletNumber") String walletNumber,
            @RequestBody FreezeWalletRequest request
    );

    /**
     * Obtenir le statut du wallet
     */
//    @GetMapping("/{walletNumber}/status")
//    @CircuitBreaker(name = "walletService")
//    WalletStatusResponse getWalletStatus(@PathVariable("walletNumber") String walletNumber);

    /**
     * Supprimer un wallet (pour compensation)
     */
    @DeleteMapping("/{walletNumber}")
    void deleteWallet(@PathVariable("walletNumber") String walletNumber);
}