package com.zaphira.service_user.client;

import com.zaphira.service_user.dto.response.WalletResponse;
import com.zaphira.service_user.exception.WalletServiceException;
import com.zaphira.common.dto.request.FreezeWalletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class WalletServiceClientFallbackFactory implements FallbackFactory<WalletServiceClient> {

    @Override
    public WalletServiceClient create(Throwable cause) {
        log.error("Wallet service fallback activated", cause);

        return new WalletServiceClient() {

            @Override
            public WalletResponse createWallet(Long userId) {
                log.error("Fallback: Cannot create wallet for user {}: {}",
                        userId, cause.getMessage());

                throw new WalletServiceException(
                        "Unable to create wallet at this time. Please try again later.",
                        cause
                );
            }

            @Override
            public WalletResponse autoCreateWallet(Long userId) {
                log.error("Fallback: Cannot auto-create wallet for user {}: {}",
                        userId, cause.getMessage());

                throw new WalletServiceException(
                        "Unable to create wallet at this time. Please try again later.",
                        cause
                );
            }

            @Override
            public WalletResponse getWallet(String walletNumber) {
                log.warn("Fallback: Cannot get wallet {}: {}", walletNumber, cause.getMessage());

                // Retourner un wallet "dégradé" depuis le cache si disponible
                return getWalletFromCache(walletNumber);
            }

            @Override
            public List<WalletResponse> getUserWallets(Long userId) {
                log.warn("Fallback: Cannot get wallets for user {}: {}", userId, cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public WalletResponse getPrimaryWallet(Long userId) {
                log.warn("Fallback: Cannot get primary wallet for user {}: {}",
                        userId, cause.getMessage());

                return getWalletFromCache("user:" + userId + ":primary");
            }

            @Override
            public WalletResponse freezeWallet(String walletNumber, FreezeWalletRequest request) {
                log.error("Fallback: Cannot freeze wallet {}: {}", walletNumber, cause.getMessage());

                throw new WalletServiceException(
                        "Unable to freeze wallet at this time. Please try again later.",
                        cause
                );
            }
//
//            @Override
//            public WalletStatusResponse getWalletStatus(String walletNumber) {
//                log.warn("Fallback: Cannot get wallet status for {}: {}",
//                        walletNumber, cause.getMessage());
//
//                // Retourner un statut "inconnu"
//                return WalletStatusResponse.builder()
//                        .walletNumber(walletNumber)
//                        .status("UNKNOWN")
//                        .message("Wallet service temporarily unavailable")
//                        .build();
//            }

            @Override
            public void deleteWallet(String walletNumber) {
                log.error("Fallback: Cannot delete wallet {}: {}", walletNumber, cause.getMessage());
                // Ne rien faire en fallback - logger pour traitement manuel
            }

            // Méthode utilitaire pour récupérer depuis le cache
            private WalletResponse getWalletFromCache(String key) {
                // TODO: Implémenter la récupération depuis Redis
                log.warn("Cache lookup not implemented for key: {}", key);
                return null;
            }
        };
    }
}