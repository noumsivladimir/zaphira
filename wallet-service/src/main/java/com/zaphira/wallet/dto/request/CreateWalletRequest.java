package com.zaphira.wallet.dto.request;

import com.zaphira.wallet.models.enums.WalletStatus;
import com.zaphira.wallet.models.enums.WalletType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWalletRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
    private BigDecimal availableBalance ;
    private WalletType type ;
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

//
//    private WalletType type;
//
//
//    @Positive(message = "Monthly limit must be positive")
//    private BigDecimal monthlyLimit;
//
//    private Boolean isPrimary;
//
//    private String metadata;
}