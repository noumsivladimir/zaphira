package com.zaphira.transaction.integration.wallet;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class WalletTransferRequest {

    private String reference;
    private String senderWalletNumber;
    private String receiverWalletNumber;
    private BigDecimal amount;
    private String currency;
    private String description;
}


