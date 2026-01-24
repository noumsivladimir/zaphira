package com.zaphira.service_user.client;

import com.zaphira.service_user.dto.request.CreateWalletRequest;
import com.zaphira.service_user.dto.response.WalletResponse;
import com.zaphira.service_user.exception.WalletServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WalletServiceClientFallbackFactory implements FallbackFactory<WalletServiceClient> {

    @Override
    public WalletServiceClient create(Throwable cause) {
        log.error("Wallet service fallback activated", cause);

        return new WalletServiceClient() {

            @Override
            public WalletResponse createWallet(CreateWalletRequest request) {
            log.error("Fallback: Cannot create wallet for user {}: {}",
                request != null ? request.getUserId() : null, cause.getMessage());

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

            // Méthode utilitaire pour récupérer depuis le cache
            private WalletResponse getWalletFromCache(String key) {
                // TODO: Implémenter la récupération depuis Redis
                log.warn("Cache lookup not implemented for key: {}", key);
                return null;
            }
        };
    }
}