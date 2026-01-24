package com.zaphira.service_user.client;


import com.zaphira.service_user.dto.request.CreateWalletRequest;
import com.zaphira.service_user.dto.response.WalletResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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
        WalletResponse createWallet(@RequestBody CreateWalletRequest request);

    /**
     * Obtenir un wallet par numéro
     */
    @GetMapping("/{walletNumber}")
    @CircuitBreaker(name = "walletService")
    WalletResponse getWallet(@PathVariable("walletNumber") String walletNumber);
}