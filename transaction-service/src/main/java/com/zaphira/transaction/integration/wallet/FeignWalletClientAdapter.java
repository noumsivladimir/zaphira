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
        WalletDetailsResponse response = WalletDetailsResponse.builder()
                .id(wallet.getId())
                .walletNumber(wallet.getWalletNumber())
                .userId(wallet.getUserId())
                .availableBalance(wallet.getAvailableBalance())
                .blockedBalance(wallet.getBlockedBalance())
                .totalBalance(wallet.getTotalBalance())
                .currency(wallet.getCurrency())
                .type(wallet.getType())
                .status(wallet.getStatus() )
                .frozen(wallet.getActive() == null || !wallet.getActive())
                .dailyLimit(wallet.getDailyLimit())
                .dailySpent(wallet.getDailySpent())
                .monthlyLimit(wallet.getMonthlyLimit())
                .monthlySpent(wallet.getMonthlySpent())
                .frozenAt(wallet.getFrozenAt())
                .frozenBy(wallet.getFrozenBy())
                .frozenReason(wallet.getFrozenReason())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
        return response;
    }

    @Override
    public void executeTransfer(WalletTransferRequest request) {
        feignClient.executeTransfer(request);
    }
}

