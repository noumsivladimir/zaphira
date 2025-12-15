package com.zaphira.transaction.integration.wallet;

import com.zaphira.transaction.integration.wallet.dto.WalletDetailsResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for wallet integration.
 * Adapts FeignWalletClient to the WalletClient interface.
 */
@Configuration
public class WalletIntegrationConfig {

    /**
     * Create a bean implementation of WalletClient using FeignWalletClient
     */
    @Bean
    public WalletClient walletClient(FeignWalletClient feignWalletClient) {
        return new WalletClient() {
            @Override
            public WalletDetailsResponse getWalletDetails(String walletNumber) {
                // Convert WalletDTO from Feign to WalletDetailsResponse
                var walletDTO = feignWalletClient.getWalletByNumber(walletNumber);
                WalletDetailsResponse response = new WalletDetailsResponse();
                response.setWalletNumber(walletDTO.getWalletNumber());
                response.setBalance(walletDTO.getBalance());
                response.setStatus(walletDTO.getActive() ? "ACTIVE" : "INACTIVE");
                return response;
            }

            @Override
            public void executeTransfer(WalletTransferRequest request) {
                feignWalletClient.executeTransfer(request);
            }
        };
    }
}
