package com.zaphira.notification.service;

import com.zaphira.common.dto.TransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceClient {

    private final RestTemplate restTemplate;

    private static final String TRANSACTION_SERVICE_URL = "http://transaction-service";

    public TransactionDTO getTransactionById(Long transactionId) {
        try {
            String url = TRANSACTION_SERVICE_URL + "/api/transactions/" + transactionId;
            return restTemplate.getForObject(url, TransactionDTO.class);
        } catch (Exception e) {
            log.warn("Transaction service not available, cannot get transaction: {}", transactionId);
            return null;
        }
    }
}