package com.zaphira.transaction.service.fee;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class FeeCalculationResult {

    private final BigDecimal feeAmount;
    private final String feeCurrency;
    private final String feeType;
}


