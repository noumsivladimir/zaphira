package com.zaphira.wallet.mapper;

import com.zaphira.common.model.enums.Currency;
import com.zaphira.wallet.dto.WalletSummaryDTO;
import com.zaphira.wallet.dto.request.CreateSubWalletRequest;
import com.zaphira.wallet.dto.response.SubWalletResponse;
import com.zaphira.wallet.model.entities.SubWallet;
import com.zaphira.wallet.model.enums.WalletStatus;
import lombok.Builder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;


@Component
@Builder
public class SubWalletMapper {

    public SubWalletResponse toResponse(SubWallet subWallet, List <WalletSummaryDTO> managingWallets) {
        if (subWallet == null) {
            return null;
        }

        return SubWalletResponse.builder()
                .id(subWallet.getId())
                .type(subWallet.getType())
                .subWalletName(subWallet.getSubWalletName())
                .currency(subWallet.getCurrency())
                .status(subWallet.getStatus())
                .availableBalance(subWallet.getAvailableBalance())
                .blockedBalance(subWallet.getBlockedBalance())
                .totalBalance(subWallet.getTotalBalance())
                .dailyLimit(subWallet.getDailyLimit())
                .monthlyLimit(subWallet.getMonthlyLimit())
                .isDefault(subWallet.getIsDefault())
                .createdAt(subWallet.getCreatedAt())
                .updatedAt(subWallet.getUpdatedAt())
                .managingWallets(managingWallets)
                .build();
    }

    public SubWalletResponse toResponse(SubWallet subWallet) {
        if (subWallet == null) {
            return null;
        }

        return SubWalletResponse.builder()
                .subWalletName(subWallet.getSubWalletName())
                .type(subWallet.getType())
                .currency(subWallet.getCurrency())
                .status(subWallet.getStatus())
                .availableBalance(subWallet.getAvailableBalance())
                .blockedBalance(subWallet.getBlockedBalance())
                .totalBalance(subWallet.getTotalBalance())
                .dailyLimit(subWallet.getDailyLimit())
                .monthlyLimit(subWallet.getMonthlyLimit())
                .isDefault(subWallet.getIsDefault())
                .build();
    }

    public SubWallet toEntity(CreateSubWalletRequest request) {
        if (request == null) {
            return null;
        }

        return SubWallet.builder()
                .subWalletName(request.getSubWalletName())
                .type(request.getType())
                .currency(Currency.XAF)
                .status(WalletStatus.ACTIVE)
                .availableBalance(BigDecimal.ZERO)
                .blockedBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .isDefault(false)
                .build();
    }

//    public List<SubWalletResponse> toResponseList(List<SubWallet> subWallets) {
//        if (subWallets == null) {
//            return List.of();
//        }
//
//        return subWallets.stream()
//                .map(this::toResponse)
//                .collect(Collectors.toList());
//    }

//    private List<SubWalletResponse.WalletSummaryDTO> mapManagingWallets(SubWallet subWallet) {
//        if (subWallet.getWalletSubWallets() == null) {
//            return List.of();
//        }
//
//        return subWallet.getWalletSubWallets().stream()
//                .map(link -> SubWalletResponse.WalletSummary.builder()
//                        .id(link.getWallet().getId())
//                        .walletNumber(link.getWallet().getWalletNumber())
//                        .type(link.getWallet().getType())
//                        .build())
//                .collect(Collectors.toList());
//    }



    static class SubWalletSummaryDTO{
   
    }
}