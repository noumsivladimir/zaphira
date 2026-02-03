package com.zaphira.wallet.dto.response;

import com.zaphira.common.model.enums.Currency;
import com.zaphira.wallet.dto.WalletSummaryDTO;
import com.zaphira.wallet.models.enums.SubWalletType;
import com.zaphira.wallet.models.enums.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubWalletResponse {

    private Long id;
    @Builder.Default
    private List<WalletSummaryDTO> managingWallets = new ArrayList<>();
    private SubWalletType type; // CHECKING, SAVINGS, BUSINESS, INVESTMENT, ESCROW
    private String subWalletName; // Nom personnalisé par l'utilisateur
    @Builder.Default
    private Currency currency = Currency.XAF;
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;
@Builder.Default
    private BigDecimal blockedBalance = BigDecimal.ZERO;
@Builder.Default
    private BigDecimal totalBalance = BigDecimal.ZERO;

    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
@Builder.Default
    private Boolean isDefault = false;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String metadata;
}
