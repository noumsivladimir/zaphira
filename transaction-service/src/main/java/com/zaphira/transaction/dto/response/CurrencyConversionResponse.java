package com.zaphira.transaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyConversionResponse {
    private BigDecimal sourceAmount;
    private String sourceCurrency;
    private BigDecimal convertedAmount;
    private String targetCurrency;
    private BigDecimal exchangeRate;
    private String provider;
}
