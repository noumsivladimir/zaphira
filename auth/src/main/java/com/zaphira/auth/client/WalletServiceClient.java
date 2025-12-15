package com.zaphira.auth.client;

import com.zaphira.common.dto.WalletDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * FeignClient pour communiquer avec wallet-service.
 * Utilisé pour créer et récupérer des wallets de manière synchrone.
 * L'URL est configurable via application.properties.
 */
@FeignClient(
    name = "wallet-service",
    url = "${wallet.service.url}" // Configure l'URL dans application.properties
)
public interface WalletServiceClient {

    /**
     * Crée un nouveau wallet pour un utilisateur.
     *
     * @param userId   L'ID de l'utilisateur
     * @param currency La devise (par défaut XOF)
     * @return WalletDTO créé
     */
    @PostMapping("/api/wallets")
    WalletDTO createWallet(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "currency", defaultValue = "XOF") String currency
    );

    /**
     * Récupère le wallet d'un utilisateur par son ID.
     *
     * @param userId L'ID de l'utilisateur
     * @return WalletDTO
     */
    @GetMapping("/api/wallets/user/{userId}")
    WalletDTO getWalletByUserId(@PathVariable("userId") Long userId);

    /**
     * Récupère un wallet par son numéro.
     *
     * @param walletNumber Numéro du wallet
     * @return WalletDTO
     */
    @GetMapping("/api/wallets/{walletNumber}")
    WalletDTO getWalletByNumber(@PathVariable("walletNumber") String walletNumber);
}
