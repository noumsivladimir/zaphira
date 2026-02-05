package com.zaphira.transaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateHistoryDTO {
    private Long id;
    private String sourceCurrency;
    private String targetCurrency;
    private BigDecimal rate;
    private BigDecimal bid;
    private BigDecimal ask;
    private String provider;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
