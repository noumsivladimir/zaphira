package com.zaphira.transaction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "transaction.fees")
@Getter
@Setter
public class FeeProperties {

    /**
     * Fixed fee applied to every transaction (e.g. 0.0 for none).
     */
    private BigDecimal fixed = BigDecimal.ZERO;

    /**
     * Percentage fee (e.g. 0.005 = 0.5%).
     */
    private BigDecimal percentage = BigDecimal.ZERO;

    /**
     * Maximum fee cap.
     */
    private BigDecimal max = BigDecimal.valueOf(1000);

    /**
     * Currency of the fees (usually same as transaction).
     */
    private String currency = "XOF";
}


