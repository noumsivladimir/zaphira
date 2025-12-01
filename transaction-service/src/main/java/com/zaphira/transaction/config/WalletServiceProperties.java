package com.zaphira.transaction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wallet.service")
@Getter
@Setter
public class WalletServiceProperties {

    /**
     * Base URL of the wallet service (e.g. http://wallet-service:8081).
     */
    private String baseUrl;

    /**
     * Path for retrieving wallet details (e.g. /api/wallets/{walletNumber}).
     */
    private String walletDetailsPath = "/api/wallets/{walletNumber}";

    /**
     * Path for executing transfers (e.g. /api/wallets/transfer).
     */
    private String transferPath = "/api/wallets/transfer";

    /**
     * Request timeout in milliseconds.
     */
    private long timeoutMs = 5000;
}


