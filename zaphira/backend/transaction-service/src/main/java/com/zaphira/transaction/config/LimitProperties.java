package com.zaphira.transaction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "transaction.limits")
@Getter
@Setter
public class LimitProperties {

    private BigDecimal perTransaction = BigDecimal.valueOf(5000);
    private BigDecimal daily = BigDecimal.valueOf(20000);
    private BigDecimal weekly = BigDecimal.valueOf(50000);
    private BigDecimal monthly = BigDecimal.valueOf(150000);

    /**
     * Amount threshold above which an additional authorization is required.
     */
    private BigDecimal authorizationThreshold = BigDecimal.valueOf(1000);

    private AuthorizationProperties authorization = new AuthorizationProperties();

    @Getter
    @Setter
    public static class AuthorizationProperties {
        private Duration expiry = Duration.ofMinutes(5);
        private AuthorizationMode defaultMode = AuthorizationMode.OTP;
        private boolean exposeChallengeInResponse = true; // useful for tests/demos
    }

    public enum AuthorizationMode {
        OTP,
        PIN,
        ADMIN
    }
}


