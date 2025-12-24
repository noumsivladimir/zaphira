package com.zaphira.wallet.mapper;

import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.models.entities.Wallet;
import org.springframework.stereotype.Component;


@Component
public class WalletMapperImpl implements WalletMapper{
    @Override
    public WalletDTO toDTO(Wallet wallet) {

        if (wallet == null) {
            return null;
        }

        WalletDTO walletDTO = WalletDTO.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .walletNumber(wallet.getWalletNumber())
                .type(wallet.getType())
                .status(wallet.getStatus())
                .subWallets(wallet.getSubWallets())
                .availableBalance(wallet.getAvailableBalance())
                .blockedBalance(wallet.getBlockedBalance())
                .totalBalance(wallet.getTotalBalance())
                .dailyLimit(wallet.getDailyLimit())
                .monthlyLimit(wallet.getMonthlyLimit())
                .dailySpent(wallet.getDailySpent())
                .monthlySpent(wallet.getMonthlySpent())
                .isPrimary(wallet.getIsPrimary())
                .frozenReason(wallet.getFrozenReason())
                .frozenAt(wallet.getFrozenAt())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .metadata(wallet.getMetadata())
                .build();


        return walletDTO;
    }



    @Override
    public Wallet toEntity(WalletDTO walletDTO) {
        return null;
    }
}
