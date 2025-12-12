package com.zaphira.transaction.integration.wallet.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WalletDetailsResponse {
    private String walletNumber;
    private BigDecimal balance;
    private String currency;
    private String status;
    private boolean frozen;
}


