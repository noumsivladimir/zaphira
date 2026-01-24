package com.zaphira.transaction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "transaction.compliance")
@Getter
@Setter
public class ComplianceProperties {

    /**
     * Seuil montant au-dessus duquel une transaction passe en UNDER_REVIEW.
     */
    private BigDecimal highAmountThreshold = BigDecimal.valueOf(1_000_000); // ex: 1M XOF

    /**
     * Active le blocage automatique des transactions jugées à très haut risque.
     */
    private boolean autoBlockHighRisk = false;
}


