package com.zaphira.transaction.integration.wallet;

import com.zaphira.transaction.integration.wallet.dto.WalletDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeignWalletClientAdapter implements WalletClient {

    private final FeignWalletClient feignClient;

    @Override
    public WalletDetailsResponse getWalletDetails(String walletNumber) {
        var wallet = feignClient.getWalletByNumber(walletNumber);
        WalletDetailsResponse response = new WalletDetailsResponse();
        response.setWalletNumber(wallet.getWalletNumber());
        response.setBalance(wallet.getBalance());
        response.setStatus(wallet.getActive() ? "ACTIVE" : "INACTIVE");
        response.setFrozen(false);
        return response;
    }

    @Override
    public void executeTransfer(WalletTransferRequest request) {
        feignClient.executeTransfer(request);
    }
}

