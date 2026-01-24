package com.zaphira.common.dto.response;

import com.zaphira.common.model.enums.WalletStatus;
import com.zaphira.common.model.enums.WalletType;
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
}

