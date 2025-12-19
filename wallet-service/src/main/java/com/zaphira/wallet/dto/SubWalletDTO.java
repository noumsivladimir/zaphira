package com.zaphira.wallet.dto;

import com.zaphira.common.model.enums.Currency;

import java.math.BigDecimal;

public class SubWalletDTO {
    private String subWalletId;
    private String subWalletName;
    //private Integer totalSubWallets;
    //private List<SubWallet> wallets;
    private BigDecimal balance;
    private Currency primaryCurrency;
}