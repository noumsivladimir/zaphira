package com.zaphira.wallet.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class WalletHierarchyDTO {
    private WalletDTO wallet;
    private List<WalletHierarchyDTO> subWallets;
    private Integer depth;
    private BigDecimal totalBalanceIncludingChildren;
}