package com.zaphira.wallet.dto.response;

import com.zaphira.wallet.models.enums.WalletStatus;
import com.zaphira.wallet.models.enums.WalletType;
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
}

