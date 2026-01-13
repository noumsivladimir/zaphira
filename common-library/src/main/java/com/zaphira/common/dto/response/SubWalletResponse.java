package com.zaphira.common.dto.response;

import com.zaphira.common.model.enums.Currency;
import com.zaphira.common.dto.WalletSummaryDTO;
import com.zaphira.common.model.enums.SubWalletType;
import com.zaphira.common.model.enums.WalletStatus;
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
    private List<WalletSummaryDTO> managingWallets = new ArrayList<>();
    private SubWalletType type; // CHECKING, SAVINGS, BUSINESS, INVESTMENT, ESCROW
    private String subWalletName; // Nom personnalisé par l'utilisateur
    private Currency currency = Currency.XAF;
    private WalletStatus status = WalletStatus.ACTIVE;
    private BigDecimal availableBalance = BigDecimal.ZERO;
    private BigDecimal blockedBalance = BigDecimal.ZERO;
    private BigDecimal totalBalance = BigDecimal.ZERO;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private Boolean isDefault = false;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String metadata;
}
