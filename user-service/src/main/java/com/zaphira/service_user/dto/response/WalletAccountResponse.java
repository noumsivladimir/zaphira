package com.zaphira.service_user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletAccountResponse {
    private String accountNumber;
    private String accountName;
    private String accountType;
    private String currency;
    private BigDecimal availableBalance;
    private Boolean isDefault;
}