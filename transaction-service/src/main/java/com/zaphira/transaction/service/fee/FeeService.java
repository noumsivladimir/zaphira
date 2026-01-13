package com.zaphira.transaction.service.fee;

import com.zaphira.transaction.config.FeeProperties;
import com.zaphira.transaction.dto.requests.TransactionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FeeService {

    private final FeeProperties properties;

    public FeeService(FeeProperties properties) {
        this.properties = properties;
    }

    public FeeCalculationResult calculateFee(TransactionRequest request) {
        BigDecimal amount = request.getAmount();
        BigDecimal fee = properties.getFixed();

        if (properties.getPercentage() != null && properties.getPercentage().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentFee = amount.multiply(properties.getPercentage())
                    .setScale(4, RoundingMode.HALF_UP);
            fee = fee.add(percentFee);
        }

        if (fee.compareTo(properties.getMax()) > 0) {
            fee = properties.getMax();
        }

        if (fee.compareTo(BigDecimal.ZERO) <= 0) {
            return FeeCalculationResult.builder()
                    .feeAmount(BigDecimal.ZERO)
                    .feeCurrency(request.getCurrency())
                    .feeType("NONE")
                    .build();
        }

        return FeeCalculationResult.builder()
                .feeAmount(fee)
                .feeCurrency(properties.getCurrency() != null ? properties.getCurrency() : request.getCurrency())
                .feeType("FIXED+PERCENTAGE")
                .build();
    }
}


