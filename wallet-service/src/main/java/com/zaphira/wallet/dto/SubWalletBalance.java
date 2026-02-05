package com.zaphira.wallet.dto;

import com.zaphira.common.model.enums.Currency;
import com.zaphira.wallet.model.enums.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour le solde d'un sub-wallet dans le résumé
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubWalletBalance {

    private String walletNumber;

    private String walletName;

    private Currency currency;

    private BigDecimal balance;

    private WalletStatus status;

    private Integer level; // Niveau dans la hiérarchie
}