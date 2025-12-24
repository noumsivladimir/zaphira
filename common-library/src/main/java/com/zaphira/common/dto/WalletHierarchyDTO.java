package com.zaphira.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletHierarchyDTO {
    private WalletDTO wallet;
    private List<WalletHierarchyDTO> subWallets;
    private Integer depth;
    private BigDecimal totalBalanceIncludingChildren;
}