package com.zaphira.common.dto;

import com.zaphira.common.model.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubWalletDTO {
    private String subWalletId;
    private String subWalletName;
    private BigDecimal balance;
    private Currency primaryCurrency;
}