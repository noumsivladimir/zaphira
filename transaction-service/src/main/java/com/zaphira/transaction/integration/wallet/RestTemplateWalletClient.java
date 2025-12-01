package com.zaphira.transaction.integration.wallet;

import com.zaphira.transaction.config.WalletServiceProperties;
import com.zaphira.transaction.integration.wallet.dto.WalletDetailsResponse;
import com.zaphira.transaction.service.exception.WalletOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateWalletClient implements WalletClient {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateWalletClient.class);

    private final RestTemplate restTemplate;
    private final WalletServiceProperties properties;

    public RestTemplateWalletClient(RestTemplate restTemplate, WalletServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public WalletDetailsResponse getWalletDetails(String walletNumber) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            log.warn("Wallet service base url not configured. Returning stubbed wallet details for {}", walletNumber);
            WalletDetailsResponse response = new WalletDetailsResponse();
            response.setWalletNumber(walletNumber);
            response.setCurrency("XOF");
            response.setFrozen(false);
            response.setStatus("ACTIVE");
            response.setBalance(java.math.BigDecimal.valueOf(1_000_000));
            return response;
        }

        String url = properties.getBaseUrl() + properties.getWalletDetailsPath();
        ResponseEntity<WalletDetailsResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                WalletDetailsResponse.class,
                walletNumber
        );
        return response.getBody();
    }

    @Override
    public void executeTransfer(WalletTransferRequest request) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            log.warn("Wallet service base url not configured. Skipping transfer {}.", request.getReference());
            return;
        }

        String url = properties.getBaseUrl() + properties.getTransferPath();
        ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(request),
                Void.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new WalletOperationException("Wallet transfer failed with status: " + response.getStatusCode());
        }
    }
}


