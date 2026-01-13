package com.zaphira.transaction.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//pour feign
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {

    private Long id;

    private Long userId;

    private String walletNumber;

    private String walletType;

    private String status;

    private String currency;

    private BigDecimal availableBalance;

    private BigDecimal blockedBalance;

    private BigDecimal totalBalance;

    private Boolean isPrimary;

    private LocalDateTime createdAt;
}