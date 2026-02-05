package com.zaphira.wallet.dto.response;

import com.zaphira.wallet.model.enums.WalletStatus;
import com.zaphira.wallet.model.enums.WalletType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletResponse {

    private Long id;
    private Long userId;
    private String walletNumber;
    private WalletType type;
    private WalletStatus status;
    private String merchantCode;
    private String merchantName;
    private java.math.BigDecimal availableBalance;
    private java.math.BigDecimal blockedBalance;
    private String currency;
    private java.math.BigDecimal totalBalance;
    private Boolean active;
}

