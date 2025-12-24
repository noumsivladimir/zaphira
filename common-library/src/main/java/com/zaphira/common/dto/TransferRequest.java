package com.zaphira.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    private String senderWalletNumber;
    private String receiverWalletNumber;
    private BigDecimal amount;
    private String currency;
    private String reference;
    private String description;
}

